package com.example.travelapp.ui.catalog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

    val durataIniziale = (itinerarioDaModificare?.durataGiorni ?: 7).coerceAtLeast(1)

    /*
     * In modifica le date NON sono quelle gia' pubblicate: una partenza venduta non si
     * sposta. I campi descrivono sempre una partenza nuova, quindi partono da una data
     * futura anche quando l'itinerario ne ha gia' altre.
     */
    val inizioIniziale = remember { oggiMillis + 7L * GIORNO_MILLIS }
    val fineIniziale = remember { inizioIniziale + (durataIniziale - 1) * GIORNO_MILLIS }

    var dataInizioMillis by remember { mutableStateOf(inizioIniziale) }
    var dataFineMillis by remember { mutableStateOf(fineIniziale) }
    var dataLimiteMillis by remember { mutableStateOf<Long?>(null) }

    // Creando un itinerario la prima partenza e' obbligatoria; modificandolo aggiungerne
    // una e' una scelta, e di norma si sta cambiando altro (prezzo, foto, descrizione).
    var aggiungiPartenza by remember { mutableStateOf(!isModifica) }

    var mostraDatePickerInizio by remember { mutableStateOf(false) }
    var mostraDatePickerFine by remember { mutableStateOf(false) }
    var mostraDatePickerLimite by remember { mutableStateOf(false) }

    var titolo by remember { mutableStateOf(itinerarioDaModificare?.titolo ?: "") }
    var descrizione by remember { mutableStateOf(itinerarioDaModificare?.descrizione ?: "") }
    var destinazione by remember { mutableStateOf(itinerarioDaModificare?.destinazionePrincipale ?: "") }
    var prezzoInput by remember { mutableStateOf(itinerarioDaModificare?.prezzoBase?.toString() ?: "") }
    var maxPartecipantiInput by remember { mutableStateOf(itinerarioDaModificare?.maxPartecipanti?.toString() ?: "20") }
    var immaginiUri by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            immaginiUri = (immaginiUri + uris).distinct()
        }
    }

    val prezzoNumerico = prezzoInput.replace(",", ".").toDoubleOrNull()
    val isPrezzoValido = prezzoNumerico != null && prezzoNumerico > 0.0

    val durataCalcolata = (((dataFineMillis - dataInizioMillis) / GIORNO_MILLIS) + 1).toInt()
    val isInizioValido = dataInizioMillis >= oggiMillis
    val isPeriodoValido = isInizioValido && dataFineMillis >= dataInizioMillis
    val isLimiteValido = dataLimiteMillis?.let { it in oggiMillis..dataInizioMillis } ?: true

    val partecipantiNumerici = maxPartecipantiInput.toIntOrNull()
    val isPartecipantiValidi = partecipantiNumerici != null && partecipantiNumerici > 0

    val isPeriodoRichiestoValido = !aggiungiPartenza || (isPeriodoValido && isLimiteValido)

    val isFormValido = titolo.isNotBlank() && destinazione.isNotBlank() && isPrezzoValido &&
            isPeriodoRichiestoValido && isPartecipantiValidi

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
            Text(
                text = "FOTO DELL'ITINERARIO",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TravelTextMuted
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TravelTextMuted, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Aggiungi foto", color = TravelTextMuted, fontSize = 12.sp)
                    }
                }

                // Foto già esistenti in modifica (se presenti e nessuna nuova selezionata o da affiancare)
                if (immaginiUri.isEmpty() && isModifica) {
                    itinerarioDaModificare.immagini.forEach { img ->
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = img.url,
                                contentDescription = "Foto salvata",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Nuove foto selezionate localmente
                immaginiUri.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Foto ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { immaginiUri = immaginiUri.filterIndexed { i, _ -> i != index } },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Rimuovi foto",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
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

            if (isModifica) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aggiungi una nuova partenza",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TravelTextDark
                        )
                        Text(
                            text = "Le partenze gia' pubblicate non si modificano: chi ha " +
                                    "prenotato conta su quelle date. Puoi eliminarle dalla " +
                                    "schermata delle partenze dell'itinerario.",
                            color = TravelTextMuted,
                            fontSize = 13.sp
                        )
                    }

                    Switch(
                        checked = aggiungiPartenza,
                        onCheckedChange = { aggiungiPartenza = it }
                    )
                }
            }

            if (aggiungiPartenza) {
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
            }

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
                                // Senza una nuova partenza non si inviano date: il server
                                // le interpreterebbe come una partenza in piu'. La durata
                                // va comunque mandata, perche' deve restare determinabile.
                                dataInizio = if (aggiungiPartenza) isoDateFormat.format(Date(dataInizioMillis)) else null,
                                dataFine = if (aggiungiPartenza) isoDateFormat.format(Date(dataFineMillis)) else null,
                                dataLimitePrenotazione = if (aggiungiPartenza) {
                                    dataLimiteMillis?.let { isoDateFormat.format(Date(it)) }
                                } else {
                                    null
                                },
                                durataGiorni = if (aggiungiPartenza) null else durataIniziale,
                                maxPartecipanti = partecipantiNumerici!!
                            ),
                            immaginiUri = immaginiUri
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