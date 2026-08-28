package com.example.travelapp.ui.catalog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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

    var titolo by remember { mutableStateOf(itinerarioDaModificare?.titolo ?: "") }
    var descrizione by remember { mutableStateOf(itinerarioDaModificare?.descrizione ?: "") }
    var destinazione by remember { mutableStateOf(itinerarioDaModificare?.destinazionePrincipale ?: "") }
    var prezzoInput by remember { mutableStateOf(itinerarioDaModificare?.prezzoBase?.toString() ?: "") }
    var durataInput by remember { mutableStateOf(itinerarioDaModificare?.durataGiorni?.toString() ?: "") }
    var maxPartecipantiInput by remember { mutableStateOf(itinerarioDaModificare?.maxPartecipanti?.toString() ?: "20") }
    var immagineUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            immagineUri = uri
        }
    }

    val prezzoNumerico = prezzoInput.replace(",", ".").toDoubleOrNull()
    val isPrezzoValido = prezzoNumerico != null && prezzoNumerico > 0.0

    val durataNumerica = durataInput.toIntOrNull()
    val isDurataValida = durataNumerica != null && durataNumerica > 0

    val partecipantiNumerici = maxPartecipantiInput.toIntOrNull()
    val isPartecipantiValidi = partecipantiNumerici != null && partecipantiNumerici > 0

    val isFormValido = titolo.isNotBlank() && destinazione.isNotBlank() && isPrezzoValido && isDurataValida && isPartecipantiValidi

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
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
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
                    value = durataInput,
                    onValueChange = { input -> if (input.all { it.isDigit() }) durataInput = input },
                    label = { Text("DURATA (GIORNI)") },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = durataInput.isNotEmpty() && !isDurataValida
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
                                prezzoBase = BigDecimal.valueOf(prezzoNumerico!!),
                                durataGiorni = durataNumerica!!,
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