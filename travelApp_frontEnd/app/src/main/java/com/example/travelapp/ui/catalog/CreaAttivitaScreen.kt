package com.example.travelapp.ui.catalog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private fun formatterIso() =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

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
    var immaginiUri by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            immaginiUri = (immaginiUri + uris).distinct()
        }
    }

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

    if (mostraDatePickerInizio) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dataInizioMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= oggiMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostraDatePickerInizio = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dataInizioMillis = it
                        if (dataFineMillis < it) dataFineMillis = it
                    }
                    mostraDatePickerInizio = false
                }) { Text("OK", color = TravelBlue) }
            },
            dismissButton = {
                TextButton(onClick = { mostraDatePickerInizio = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState, title = { Text("Seleziona data inizio validità", modifier = Modifier.padding(16.dp)) })
        }
    }

    if (mostraDatePickerFine) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dataFineMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= dataInizioMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostraDatePickerFine = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dataFineMillis = it }
                    mostraDatePickerFine = false
                }) { Text("OK", color = TravelBlue) }
            },
            dismissButton = {
                TextButton(onClick = { mostraDatePickerFine = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState, title = { Text("Seleziona data fine validità", modifier = Modifier.padding(16.dp)) })
        }
    }

    LaunchedEffect(uiState.salvataggioCompletato) {
        if (uiState.salvataggioCompletato) {
            Toast.makeText(
                context,
                if (isModifica) "Attività modificata con successo!" else "Attività creata con successo!",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TravelSurface)
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
            Text(
                text = "FOTO DELL'ATTIVITÀ",
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
                        .background(ImagePlaceholder)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TravelTextMuted, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Aggiungi foto", color = TravelTextMuted, fontSize = 12.sp)
                    }
                }

                if (immaginiUri.isEmpty() && isModifica) {
                    attivitaDaModificare.immagini.forEach { img ->
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
                label = { Text("TITOLO ATTIVITÀ") },
                placeholder = { Text("Es. Escursione guidata") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = luogo,
                onValueChange = { if (it.length <= 150) luogo = it },
                label = { Text("LUOGO / PUNTO DI RITROVO") },
                placeholder = { Text("Es. Rifugio Auronzo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = descrizione,
                onValueChange = { if (it.length <= 5000) descrizione = it },
                label = { Text("DESCRIZIONE") },
                placeholder = { Text("Dettagli dell'esperienza...") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
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
                    label = { Text("PREZZO (€)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = prezzoInput.isNotEmpty() && !isPrezzoValido
                )

                OutlinedTextField(
                    value = postiInput,
                    onValueChange = { input -> if (input.all { it.isDigit() }) postiInput = input },
                    label = { Text("MAX POSTI") },
                    placeholder = { Text("15") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = postiInput.isNotEmpty() && !isPostiValidi
                )

                OutlinedTextField(
                    value = durataOreInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' || it == ',' }) durataOreInput = input
                    },
                    label = { Text("DURATA (ORE)") },
                    placeholder = { Text("2.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = durataOreInput.isNotEmpty() && !isDurataValida
                )
            }

            Text("GIORNI DISPONIBILI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TravelTextMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                giorniSettimana.forEach { (nome, index) ->
                    val isSelezionato = index in giorniSelezionati
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSelezionato) TravelBlue else TravelSurface)
                            .border(1.dp, if (isSelezionato) TravelBlue else Color.LightGray, CircleShape)
                            .clickable {
                                giorniSelezionati = if (isSelezionato) {
                                    giorniSelezionati - index
                                } else {
                                    giorniSelezionati + index
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = nome,
                            color = if (isSelezionato) Color.White else TravelTextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Text("PERIODO DI VALIDITÀ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TravelTextMuted)
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
                    modifier = Modifier.weight(1f).clickable { mostraDatePickerInizio = true }
                )

                OutlinedTextField(
                    value = displayDateFormat.format(Date(dataFineMillis)),
                    onValueChange = {},
                    readOnly = true,
                    isError = !isDateRangeValido,
                    label = { Text("DATA FINE") },
                    trailingIcon = {
                        IconButton(onClick = { mostraDatePickerFine = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Scegli data fine", tint = TravelBlue)
                        }
                    },
                    modifier = Modifier.weight(1f).clickable { mostraDatePickerFine = true }
                )
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    if (isFormValido && !uiState.isSalvataggioInCorso) {
                        val durataMinuti = ((durataOre ?: 1.0) * 60).toInt()
                        viewModel.salvaAttivita(
                            context = context,
                            idDaModificare = attivitaDaModificare?.id,
                            request = SingolaAttivitaRequestDto(
                                titolo = titolo,
                                luogo = luogo,
                                descrizione = descrizione,
                                prezzo = BigDecimal(prezzoNumerico!!),
                                durataMinuti = durataMinuti,
                                maxPartecipanti = postiNumerici!!
                            ),
                            dataInizio = isoDateFormat.format(Date(dataInizioMillis)),
                            dataFine = isoDateFormat.format(Date(dataFineMillis)),
                            giorniSettimana = giorniSelezionati,
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
                            if (isModifica) "Salva Modifiche" else "Crea Attività",
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