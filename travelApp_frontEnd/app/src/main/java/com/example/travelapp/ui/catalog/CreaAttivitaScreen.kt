package com.example.travelapp.ui.catalog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
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
import com.example.travelapp.data.remote.dto.SingolaAttivitaRequestDto
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.theme.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreaAttivitaScreen(
    attivitaDaModificare: SingolaAttivita? = null,
    onBack: () -> Unit,
    viewModel: CreaAttivitaViewModel = viewModel()
) {
    val context = LocalContext.current
    val isModifica = attivitaDaModificare != null
    val uiState by viewModel.uiState.collectAsState()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val oggiMillis = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val fineDefaultMillis = remember { oggiMillis + (30L * 24 * 60 * 60 * 1000) }

    var dataInizioMillis by remember { mutableStateOf(oggiMillis) }
    var dataFineMillis by remember { mutableStateOf(fineDefaultMillis) }

    var mostraDatePickerInizio by remember { mutableStateOf(false) }
    var mostraDatePickerFine by remember { mutableStateOf(false) }

    var titolo by remember { mutableStateOf(attivitaDaModificare?.titolo ?: "") }
    var luogo by remember { mutableStateOf(attivitaDaModificare?.luogo ?: "") }
    var descrizione by remember { mutableStateOf(attivitaDaModificare?.descrizione ?: "") }
    var prezzoInput by remember { mutableStateOf(attivitaDaModificare?.prezzo?.toString() ?: "") }
    var postiInput by remember { mutableStateOf(attivitaDaModificare?.maxPartecipanti?.toString() ?: "") }
    var durataOreInput by remember {
        mutableStateOf(attivitaDaModificare?.durataMinuti?.let { (it / 60.0).toString() } ?: "")
    }
    var immagineUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> immagineUri = uri }

    val giorniSettimana = listOf("Lun" to 1, "Mar" to 2, "Mer" to 3, "Gio" to 4, "Ven" to 5, "Sab" to 6, "Dom" to 7)
    var giorniSelezionati by remember { mutableStateOf(setOf(6, 7)) }

    val prezzoNumerico = prezzoInput.replace(",", ".").toDoubleOrNull()
    val isPrezzoValido = prezzoNumerico != null && prezzoNumerico > 0.0

    val postiNumerici = postiInput.toIntOrNull()
    val isPostiValidi = postiNumerici != null && postiNumerici > 0

    val durataOre = durataOreInput.replace(",", ".").toDoubleOrNull()
    val isDurataValida = durataOre != null && durataOre > 0.0

    val isDateRangeValido = dataFineMillis >= dataInizioMillis

    val isFormValido = titolo.isNotBlank() &&
            luogo.isNotBlank() &&
            isPrezzoValido &&
            isPostiValidi &&
            isDurataValida &&
            isDateRangeValido &&
            giorniSelezionati.isNotEmpty()

    LaunchedEffect(uiState.salvataggioCompletato) {
        if (uiState.salvataggioCompletato) {
            Toast.makeText(
                context,
                if (isModifica) "Attività aggiornata con successo!" else "Attività creata con successo!",
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
                        dataInizioMillis = selected
                        if (dataFineMillis < selected) {
                            dataFineMillis = selected + (7L * 24 * 60 * 60 * 1000)
                        }
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
            DatePicker(state = datePickerState, title = { Text("Seleziona data inizio", modifier = Modifier.padding(16.dp)) })
        }
    }

    if (mostraDatePickerFine) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dataFineMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= dataInizioMillis
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
            DatePicker(state = datePickerState, title = { Text("Seleziona data fine", modifier = Modifier.padding(16.dp)) })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isModifica) "Modifica Attività" else "Crea Attività",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                if (immagineUri != null) {
                    AsyncImage(
                        model = immagineUri,
                        contentDescription = "Copertina attività",
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
                onValueChange = { titolo = it },
                label = { Text("Titolo attività") },
                placeholder = { Text("Es. Tour enogastronomico del Chianti") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = luogo,
                onValueChange = { luogo = it },
                label = { Text("Luogo di svolgimento") },
                placeholder = { Text("Es. Firenze, Chianti") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = descrizione,
                onValueChange = { descrizione = it },
                label = { Text("Descrizione") },
                placeholder = { Text("Descrivi i dettagli dell'esperienza...") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = prezzoInput,
                onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' }) prezzoInput = input },
                label = { Text("Prezzo (€)") },
                placeholder = { Text("0.00") },
                isError = prezzoInput.isNotEmpty() && !isPrezzoValido,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = postiInput,
                onValueChange = { input -> if (input.all { it.isDigit() }) postiInput = input },
                label = { Text("Posti disponibili per sessione") },
                placeholder = { Text("Es. 15") },
                isError = postiInput.isNotEmpty() && !isPostiValidi,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = durataOreInput,
                onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' }) durataOreInput = input },
                label = { Text("Durata sessione (ore)") },
                placeholder = { Text("Es. 2.5") },
                isError = durataOreInput.isNotEmpty() && !isDurataValida,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isModifica) {
                Text("Periodo di disponibilità", fontWeight = FontWeight.Bold, color = TravelTextDark, fontSize = 14.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = displayDateFormat.format(Date(dataInizioMillis)),
                        onValueChange = {},
                        readOnly = true,
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

                Text("Giorni operativi", fontWeight = FontWeight.Bold, color = TravelTextDark, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    giorniSettimana.forEach { (label, value) ->
                        val selected = giorniSelezionati.contains(value)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (selected) TravelBlue else Color.White)
                                .border(1.dp, if (selected) TravelBlue else TravelBorder, CircleShape)
                                .clickable {
                                    giorniSelezionati = if (selected) giorniSelezionati - value else giorniSelezionati + value
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else TravelTextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isFormValido && !uiState.isSalvataggioInCorso) {
                        val minutiTotali = (durataOre!! * 60).toInt()
                        val strInizio = dateFormat.format(Date(dataInizioMillis))
                        val strFine = dateFormat.format(Date(dataFineMillis))

                        viewModel.salvaAttivita(
                            context = context,
                            idDaModificare = attivitaDaModificare?.id,
                            request = SingolaAttivitaRequestDto(
                                titolo = titolo,
                                descrizione = descrizione,
                                luogo = luogo,
                                prezzo = BigDecimal(prezzoNumerico!!),
                                durataMinuti = minutiTotali,
                                maxPartecipanti = postiNumerici!!
                            ),
                            dataInizio = strInizio,
                            dataFine = strFine,
                            giorniSettimana = giorniSelezionati,
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
                    Text(
                        if (isModifica) "Salva Modifiche" else "Crea Attività",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}