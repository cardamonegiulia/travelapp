package com.example.travelapp.ui.catalog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelapp.data.remote.dto.ItinerarioRequestDto
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.ui.theme.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val GIORNO_MILLIS = 24L * 60 * 60 * 1000

// Le date viaggiano verso il backend in ISO e sono giorni pieni: le trattiamo sempre a
// mezzanotte UTC, come fa il DatePicker, per non perdere o guadagnare un giorno.
private fun formatterIso() =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

private fun dataIsoInMillis(iso: String?): Long? =
    iso?.let { runCatching { formatterIso().parse(it)?.time }.getOrNull() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreaItinerarioScreen(
    itinerarioDaModificare: Itinerario? = null,
    onBack: () -> Unit,
    viewModel: CreaItinerarioViewModel = viewModel()
) {
    val context = LocalContext.current
    val isModifica = itinerarioDaModificare != null
    val uiState by viewModel.uiState.collectAsState()

    val displayDateFormat = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
    val isoDateFormat = remember { formatterIso() }

    val oggiMillis = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // In modifica riprendiamo il periodo salvato; se l'itinerario non ne ha uno ripieghiamo
    // sulla durata gia' registrata, partendo fra una settimana.
    val durataIniziale = (itinerarioDaModificare?.durataGiorni ?: 7).coerceAtLeast(1)
    val inizioIniziale = remember {
        dataIsoInMillis(itinerarioDaModificare?.dataInizio) ?: (oggiMillis + 7L * GIORNO_MILLIS)
    }
    val fineIniziale = remember {
        dataIsoInMillis(itinerarioDaModificare?.dataFine)
            ?: (inizioIniziale + (durataIniziale - 1) * GIORNO_MILLIS)
    }

    // Il termine per prenotare e' facoltativo: se non impostato si prenota fino alla partenza.
    val limiteIniziale = remember { dataIsoInMillis(itinerarioDaModificare?.dataLimitePrenotazione) }

    var dataInizioMillis by remember { mutableStateOf(inizioIniziale) }
    var dataFineMillis by remember { mutableStateOf(fineIniziale) }
    var dataLimiteMillis by remember { mutableStateOf(limiteIniziale) }

    var mostraDatePickerInizio by remember { mutableStateOf(false) }
    var mostraDatePickerFine by remember { mutableStateOf(false) }
    var mostraDatePickerLimite by remember { mutableStateOf(false) }

    var titolo by remember { mutableStateOf(itinerarioDaModificare?.titolo ?: "") }
    var descrizione by remember { mutableStateOf(itinerarioDaModificare?.descrizione ?: "") }
    var destinazione by remember { mutableStateOf(itinerarioDaModificare?.destinazionePrincipale ?: "") }
    var prezzoInput by remember { mutableStateOf(itinerarioDaModificare?.prezzoBase?.toString() ?: "") }
    var maxPartecipantiInput by remember { mutableStateOf(itinerarioDaModificare?.maxPartecipanti?.toString() ?: "20") }
    var immagineUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> immagineUri = uri }

    val prezzoNumerico = prezzoInput.replace(",", ".").toDoubleOrNull()
    val isPrezzoValido = prezzoNumerico != null && prezzoNumerico > 0.0

    // La durata mostrata e' quella che ricavera' anche il server dalle due date, estremi inclusi.
    val durataCalcolata = (((dataFineMillis - dataInizioMillis) / GIORNO_MILLIS) + 1).toInt()
    val isInizioValido = dataInizioMillis >= oggiMillis
    val isPeriodoValido = isInizioValido && dataFineMillis >= dataInizioMillis
    // Chiudere le prenotazioni dopo la partenza non avrebbe senso: il server lo rifiuta comunque.
    val isLimiteValido = dataLimiteMillis?.let { it in oggiMillis..dataInizioMillis } ?: true

    val partecipantiNumerici = maxPartecipantiInput.toIntOrNull()
    val isPartecipantiValidi = partecipantiNumerici != null && partecipantiNumerici > 0

    val isFormValido = titolo.isNotBlank() && destinazione.isNotBlank() && isPrezzoValido && isPeriodoValido && isLimiteValido && isPartecipantiValidi

    if (mostraDatePickerInizio) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dataInizioMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= oggiMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostraDatePickerInizio = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        val durataPrecedente = (dataFineMillis - dataInizioMillis) / GIORNO_MILLIS
                        dataInizioMillis = selected
                        // Spostando l'inizio trasciniamo la fine, per non lasciare un intervallo negativo.
                        if (dataFineMillis < selected) {
                            dataFineMillis = selected + durataPrecedente.coerceAtLeast(0) * GIORNO_MILLIS
                        }
                        dataLimiteMillis = dataLimiteMillis?.coerceAtMost(selected)
                    }
                    mostraDatePickerInizio = false
                }) {
                    Text("OK", color = TravelBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostraDatePickerInizio = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = datePickerState, title = { Text("Seleziona data di inizio", modifier = Modifier.padding(16.dp)) })
        }
    }

    if (mostraDatePickerFine) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dataFineMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= maxOf(dataInizioMillis, oggiMillis)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostraDatePickerFine = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        dataFineMillis = selected
                    }
                    mostraDatePickerFine = false
                }) {
                    Text("OK", color = TravelBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostraDatePickerFine = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = datePickerState, title = { Text("Seleziona data di fine", modifier = Modifier.padding(16.dp)) })
        }
    }

    if (mostraDatePickerLimite) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dataLimiteMillis ?: dataInizioMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in oggiMillis..dataInizioMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostraDatePickerLimite = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        dataLimiteMillis = selected
                    }
                    mostraDatePickerLimite = false
                }) {
                    Text("OK", color = TravelBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostraDatePickerLimite = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Ultimo giorno per prenotare", modifier = Modifier.padding(16.dp)) }
            )
        }
    }

    LaunchedEffect(uiState.salvataggioCompletato) {
        if (uiState.salvataggioCompletato) {
            Toast.makeText(
                context,
                if (isModifica) "Itinerario aggiornato con successo!" else "Itinerario creato con successo!",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.resetStato()
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isModifica) "Modifica Itinerario" else "Crea Itinerario",
                        fontWeight = FontWeight.Bold,
                        color = TravelTextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = TravelTextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TravelBg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = itinerarioDaModificare?.immagini?.firstOrNull()?.url
                if (immagineUri != null || imageUrl != null) {
                    AsyncImage(
                        model = immagineUri ?: imageUrl,
                        contentDescription = "Copertina",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TravelTextMuted, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Aggiungi foto di copertina", color = TravelTextMuted, fontSize = 14.sp)
                    }
                }
            }

            OutlinedTextField(
                value = titolo,
                onValueChange = { if (it.length <= 150) titolo = it },
                label = { Text("TITOLO ITINERARIO") },
                placeholder = { Text("Es. Tour delle Dolomiti") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = destinazione,
                onValueChange = { if (it.length <= 150) destinazione = it },
                label = { Text("DESTINAZIONE PRINCIPALE") },
                placeholder = { Text("Es. Trentino-Alto Adige") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = descrizione,
                onValueChange = { if (it.length <= 5000) descrizione = it },
                label = { Text("DESCRIZIONE") },
                placeholder = { Text("Descrivi l'esperienza...") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = displayDateFormat.format(Date(dataInizioMillis)),
                    onValueChange = {},
                    readOnly = true,
                    isError = !isInizioValido,
                    label = { Text("DATA INIZIO") },
                    trailingIcon = {
                        IconButton(onClick = { mostraDatePickerInizio = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Scegli data inizio", tint = TravelBlue)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { mostraDatePickerInizio = true }
                )

                OutlinedTextField(
                    value = displayDateFormat.format(Date(dataFineMillis)),
                    onValueChange = {},
                    readOnly = true,
                    isError = !isPeriodoValido,
                    label = { Text("DATA FINE") },
                    trailingIcon = {
                        IconButton(onClick = { mostraDatePickerFine = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Scegli data fine", tint = TravelBlue)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { mostraDatePickerFine = true }
                )
            }

            Text(
                text = when {
                    !isInizioValido -> "La data di inizio non puo' essere nel passato"
                    !isPeriodoValido -> "La data di fine non puo' precedere quella di inizio"
                    else -> "Durata: $durataCalcolata giorni"
                },
                color = if (isPeriodoValido) TravelTextMuted else MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )

            OutlinedTextField(
                value = dataLimiteMillis?.let { displayDateFormat.format(Date(it)) }
                    ?: "Nessun limite: si prenota fino alla partenza",
                onValueChange = {},
                readOnly = true,
                isError = !isLimiteValido,
                label = { Text("PRENOTAZIONI ENTRO IL") },
                supportingText = {
                    Text(
                        if (isLimiteValido) "Facoltativo: dopo questa data l'itinerario non e' piu' prenotabile"
                        else "Il termine deve cadere fra oggi e la data di inizio"
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (dataLimiteMillis != null) {
                            IconButton(onClick = { dataLimiteMillis = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Rimuovi il termine", tint = TravelTextMuted)
                            }
                        }
                        IconButton(onClick = { mostraDatePickerLimite = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Scegli il termine", tint = TravelBlue)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostraDatePickerLimite = true }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = prezzoInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' || it == ',' }) prezzoInput = input
                    },
                    label = { Text("PREZZO BASE (€)") },
                    placeholder = { Text("€ 0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = prezzoInput.isNotEmpty() && !isPrezzoValido
                )

                OutlinedTextField(
                    value = maxPartecipantiInput,
                    onValueChange = { input -> if (input.all { it.isDigit() }) maxPartecipantiInput = input },
                    label = { Text("MAX PARTECIPANTI") },
                    placeholder = { Text("20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = maxPartecipantiInput.isNotEmpty() && !isPartecipantiValidi
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TravelChipBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TravelBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Nota: Potrai gestire foto aggiuntive e tappe in qualsiasi momento.",
                        color = TravelBlueDark,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    if (isFormValido && !uiState.isSalvataggioInCorso) {
                        viewModel.salvaItinerario(
                            context = context,
                            idDaModificare = itinerarioDaModificare?.id,
                            request = ItinerarioRequestDto(
                                titolo = titolo,
                                descrizione = descrizione,
                                destinazionePrincipale = destinazione,
                                prezzoBase = BigDecimal(prezzoNumerico!!),
                                dataInizio = isoDateFormat.format(Date(dataInizioMillis)),
                                dataFine = isoDateFormat.format(Date(dataFineMillis)),
                                dataLimitePrenotazione = dataLimiteMillis?.let { isoDateFormat.format(Date(it)) },
                                maxPartecipanti = partecipantiNumerici!!
                            ),
                            immagineUri = immagineUri
                        )
                    }
                },
                enabled = isFormValido && !uiState.isSalvataggioInCorso,
                colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSalvataggioInCorso) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isModifica) "Salva Modifiche" else "Crea Itinerario",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}