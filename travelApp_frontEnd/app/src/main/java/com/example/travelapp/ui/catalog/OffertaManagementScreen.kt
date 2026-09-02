package com.example.travelapp.ui.catalog

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferteManagementScreen(
    isAdmin: Boolean = false,
    onBack: () -> Unit,
    onModificaItinerario: (Itinerario) -> Unit = {},
    onModificaAttivita: (SingolaAttivita) -> Unit = {},
    viewModel: OfferteManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(isAdmin) {
        viewModel.caricaOfferte(soloMie = !isAdmin)
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isAdmin) "Tutte le Offerte (Admin)" else "Le mie offerte",
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TravelBg,
                contentColor = TravelBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Itinerari (${uiState.itinerari.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Attività (${uiState.attivita.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TravelBlue)
                }
            } else if (selectedTab == 0) {
                // --- TAB ITINERARI ---
                if (uiState.itinerari.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Nessun itinerario presente", color = TravelTextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.itinerari, key = { it.id }) { item ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = TravelSurface),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                    ) {
                                        AsyncImage(
                                            model = item.immagini.firstOrNull()?.url,
                                            contentDescription = item.titolo,
                                            modifier = Modifier.fillMaxSize().background(ImagePlaceholder),
                                            contentScale = ContentScale.Crop
                                        )

                                        Row(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Il tasto modifica compare SOLO se NON è Admin
                                            if (!isAdmin) {
                                                IconButton(
                                                    onClick = { onModificaItinerario(item) },
                                                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = TravelBlue, modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.eliminaItinerario(item.id) },
                                                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = item.titolo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TravelTextDark)
                                            Text(text = "${item.durataGiorni ?: 1} giorni • ${item.destinazionePrincipale ?: ""}", color = TravelTextMuted, fontSize = 13.sp)
                                        }
                                        Text(text = "€${item.prezzoBase ?: "0"}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TravelBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- TAB ATTIVITÀ SINGOLE ---
                if (uiState.attivita.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Nessuna attività presente", color = TravelTextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.attivita, key = { it.id }) { item ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = TravelSurface),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().background(ImagePlaceholder))

                                        Row(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Il tasto modifica compare SOLO se NON è Admin
                                            if (!isAdmin) {
                                                IconButton(
                                                    onClick = { onModificaAttivita(item) },
                                                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = TravelBlue, modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.eliminaAttivita(item.id) },
                                                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = item.titolo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TravelTextDark)
                                            Text(text = "${item.durataMinuti?.let { it / 60 } ?: 1}h • ${item.luogo ?: ""}", color = TravelTextMuted, fontSize = 13.sp)
                                        }
                                        Text(text = "€${item.prezzo ?: "0"}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TravelOrange)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}