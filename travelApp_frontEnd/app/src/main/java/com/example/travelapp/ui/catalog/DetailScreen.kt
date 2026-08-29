package com.example.travelapp.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.theme.*

@Composable
fun ItinerarioDetailScreen(
    itinerario: Itinerario,
    viewModel: DetailViewModel = viewModel(),
    onBack: () -> Unit,
    onPrenota: (disponibilitaId: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val copertinaUrl = itinerario.immagini.firstOrNull()?.url

    LaunchedEffect(itinerario.id) {
        viewModel.caricaDisponibilitaItinerario(itinerario.id)
    }

    Scaffold(
        bottomBar = {
            Surface(
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "A partire da",
                            style = MaterialTheme.typography.labelMedium,
                            color = TravelTextMuted
                        )
                        Text(
                            text = "€${itinerario.prezzoBase ?: "---"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TravelTextDark
                        )
                    }

                    Button(
                        onClick = { uiState.idSelezionato?.let { onPrenota(it) } },
                        enabled = uiState.idSelezionato != null,
                        colors = ButtonDefaults.buttonColors(containerColor = TravelOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Prenota",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TravelBg)
                .verticalScroll(rememberScrollState())
        ) {
            // Header con Immagine Coil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                AsyncImage(
                    model = copertinaUrl,
                    contentDescription = itinerario.titolo,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFCBD5E1)),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TravelTextDark
                        )
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Preferiti",
                            tint = TravelTextDark
                        )
                    }
                }
            }

            // Dettagli
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TravelChipBg
                ) {
                    Text(
                        text = "ITINERARIO GUIDATO",
                        color = TravelBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = itinerario.titolo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itinerario.destinazionePrincipale?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TravelTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = TravelTextMuted)
                        }
                    }

                    itinerario.durataGiorni?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = TravelTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(text = "$it giorni", style = MaterialTheme.typography.bodyMedium, color = TravelTextMuted)
                        }
                    }
                }

                HorizontalDivider(color = TravelBorder)

                // Date e Disponibilità Reali
                Text(
                    text = "Date disponibili",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TravelBlue, modifier = Modifier.size(28.dp))
                    }
                } else if (uiState.disponibilitaItinerario.isEmpty()) {
                    Text(
                        text = "Nessuna data attualmente disponibile per questo itinerario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TravelTextMuted
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(uiState.disponibilitaItinerario) { disp ->
                            val isSelected = uiState.idSelezionato == disp.id
                            SlotDateCard(
                                title = "${disp.dataInizio} - ${disp.dataFine}",
                                subtitle = "${disp.postiDisponibili} posti rimasti",
                                isSelected = isSelected,
                                onClick = { viewModel.selezionaSlot(disp.id) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = TravelBorder)

                Text(
                    text = "Descrizione del viaggio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )
                Text(
                    text = itinerario.descrizione ?: "Nessuna descrizione disponibile per questo itinerario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TravelTextMuted,
                    lineHeight = 22.sp
                )

                HorizontalDivider(color = TravelBorder)

                Text(
                    text = "Programma dell'itinerario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TappaItem(giorno = "Giorno 1", titolo = "Arrivo e check-in", desc = "Accoglienza dei partecipanti e briefing iniziale.")
                    TappaItem(giorno = "Giorno 2", titolo = "Escursione principale", desc = "Visita guidata ai punti di maggiore interesse.")
                    TappaItem(giorno = "Giorno 3", titolo = "Rientro e saluti", desc = "Tempo libero per shopping e ripartenza.")
                }
            }
        }
    }
}

@Composable
fun AttivitaDetailScreen(
    attivita: SingolaAttivita,
    viewModel: DetailViewModel = viewModel(),
    onBack: () -> Unit,
    onPrenota: (sessioneId: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(attivita.id) {
        viewModel.caricaSessioniAttivita(attivita.id)
    }

    Scaffold(
        bottomBar = {
            Surface(
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Prezzo a persona",
                            style = MaterialTheme.typography.labelMedium,
                            color = TravelTextMuted
                        )
                        Text(
                            text = "€${attivita.prezzo ?: "---"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TravelTextDark
                        )
                    }

                    Button(
                        onClick = { uiState.idSelezionato?.let { onPrenota(it) } },
                        enabled = uiState.idSelezionato != null,
                        colors = ButtonDefaults.buttonColors(containerColor = TravelOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Prenota",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TravelBg)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TravelTextDark
                        )
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Preferiti",
                            tint = TravelTextDark
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TravelChipBg
                ) {
                    Text(
                        text = "ATTIVITÀ ESPERIENZIALE",
                        color = TravelBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = attivita.titolo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                attivita.luogo?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TravelTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = TravelTextMuted)
                    }
                }

                HorizontalDivider(color = TravelBorder)

                // Sessioni Reali per Attività
                Text(
                    text = "Sessioni disponibili",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TravelBlue, modifier = Modifier.size(28.dp))
                    }
                } else if (uiState.sessioniAttivita.isEmpty()) {
                    Text(
                        text = "Nessuna sessione programmata per questa attività.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TravelTextMuted
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(uiState.sessioniAttivita) { sess ->
                            val isSelected = uiState.idSelezionato == sess.id
                            SlotDateCard(
                                title = sess.dataInizio.substringBefore("T"),
                                subtitle = "${sess.postiDisponibili} posti",
                                isSelected = isSelected,
                                onClick = { viewModel.selezionaSlot(sess.id) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = TravelBorder)

                Text(
                    text = "Descrizione dell'esperienza",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )
                Text(
                    text = attivita.descrizione ?: "Nessuna descrizione disponibile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TravelTextMuted,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun SlotDateCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) TravelBlue else TravelBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TravelChipBg else Color.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) TravelBlue else TravelTextDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TravelTextMuted
            )
        }
    }
}

@Composable
private fun TappaItem(giorno: String, titolo: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 6.dp)
                .background(TravelBlue, CircleShape)
        )
        Column {
            Text(
                text = "$giorno: $titolo",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TravelTextDark
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TravelTextMuted
            )
        }
    }
}