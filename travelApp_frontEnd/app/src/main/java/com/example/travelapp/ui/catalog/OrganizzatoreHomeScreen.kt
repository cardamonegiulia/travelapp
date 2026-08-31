package com.example.travelapp.ui.catalog

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.PrenotazioneRepository
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class OrganizzatoreHomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo = PrenotazioneRepository(
        ApiClient.getPrenotazioneApi(application)
    )

    private val _saldo =
        MutableStateFlow<BigDecimal?>(null)

    val saldo: StateFlow<BigDecimal?> =
        _saldo.asStateFlow()

    init {
        caricaSaldo()
    }

    fun caricaSaldo() {
        viewModelScope.launch {
            val result =
                repo.getSaldoOrganizzatore()

            _saldo.value =
                result.getOrDefault(
                    BigDecimal.ZERO
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizzatoreHomeScreen(
    onCreaItinerario: () -> Unit,
    onCreaAttivita: () -> Unit,
    onModificaItinerario: (Itinerario) -> Unit,
    onModificaAttivita: (SingolaAttivita) -> Unit,
    onVaiProfilo: () -> Unit,
    onLogout: () -> Unit,
    homeViewModel: OrganizzatoreHomeViewModel = viewModel(),
    offerteViewModel: OfferteManagementViewModel = viewModel()
) {

    val context =
        LocalContext.current

    val saldo by
    homeViewModel.saldo.collectAsState()

    val uiState by
    offerteViewModel.uiState.collectAsState()

    var selectedTab by
    remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        offerteViewModel.caricaOfferte(
            soloMie = true
        )

        homeViewModel.caricaSaldo()
    }

    LaunchedEffect(
        uiState.feedbackMessage
    ) {
        uiState.feedbackMessage
            ?.let {

                Toast.makeText(
                    context,
                    it,
                    Toast.LENGTH_SHORT
                ).show()

                offerteViewModel
                    .clearFeedback()
            }
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        text = "Area Organizzatore",
                        fontWeight = FontWeight.Bold,
                        color = TravelTextDark
                    )
                },

                actions = {

                    IconButton(
                        onClick = onVaiProfilo
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Person,
                            contentDescription =
                                "Profilo",
                            tint = TravelBlue
                        )
                    }

                    IconButton(
                        onClick = onLogout
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled
                                    .ExitToApp,
                            contentDescription =
                                "Logout",
                            tint =
                                Color(0xFFDC2626)
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                Color.White
                        )
            )
        },

        bottomBar = {

            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    Button(
                        onClick =
                            onCreaItinerario,
                        modifier =
                            Modifier.weight(1f),
                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        TravelBlue
                                )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Add,
                            contentDescription = null,
                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text(
                            text = "+ Itinerario",
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Button(
                        onClick =
                            onCreaAttivita,
                        modifier =
                            Modifier.weight(1f),
                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        TravelOrange
                                )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Add,
                            contentDescription = null,
                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text(
                            text = "+ Attività",
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.Bold
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
        ) {

            Card(
                shape =
                    RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            TravelBlue
                    ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(
                        text =
                            "Il tuo Saldo Guadagnato",
                        color =
                            Color.White.copy(
                                alpha = 0.8f
                            ),
                        fontSize = 13.sp
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "€ ${saldo?.toPlainString() ?: "0.00"}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            TabRow(
                selectedTabIndex =
                    selectedTab,
                containerColor =
                    Color.White,
                contentColor =
                    TravelBlue
            ) {

                Tab(
                    selected =
                        selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    text = {
                        Text(
                            text =
                                "I tuoi Itinerari (${uiState.itinerari.size})",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                )

                Tab(
                    selected =
                        selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                    },
                    text = {
                        Text(
                            text =
                                "Le tue Attività (${uiState.attivita.size})",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                )
            }

            if (uiState.isLoading) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TravelBlue
                    )
                }

            } else if (
                selectedTab == 0
            ) {

                if (
                    uiState.itinerari.isEmpty()
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                "Nessun itinerario creato. Creane uno!",
                            color =
                                TravelTextMuted
                        )
                    }

                } else {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                14.dp
                            )
                    ) {

                        items(
                            uiState.itinerari,
                            key = { it.id }
                        ) { item ->

                            Card(
                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),
                                colors =
                                    CardDefaults
                                        .cardColors(
                                            containerColor =
                                                TravelSurface
                                        ),
                                elevation =
                                    CardDefaults
                                        .cardElevation(
                                            2.dp
                                        ),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {

                                Column {

                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(
                                                    130.dp
                                                )
                                    ) {

                                        AsyncImage(
                                            model =
                                                item
                                                    .immagini
                                                    .firstOrNull()
                                                    ?.url,
                                            contentDescription =
                                                item.titolo,
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Color(
                                                            0xFFCBD5E1
                                                        )
                                                    ),
                                            contentScale =
                                                ContentScale.Crop
                                        )

                                        Row(
                                            modifier =
                                                Modifier
                                                    .align(
                                                        Alignment.TopEnd
                                                    )
                                                    .padding(
                                                        8.dp
                                                    ),
                                            horizontalArrangement =
                                                Arrangement
                                                    .spacedBy(
                                                        6.dp
                                                    )
                                        ) {

                                            IconButton(
                                                onClick = {
                                                    onModificaItinerario(
                                                        item
                                                    )
                                                },
                                                modifier =
                                                    Modifier
                                                        .size(
                                                            34.dp
                                                        )
                                                        .background(
                                                            Color.White.copy(
                                                                alpha =
                                                                    0.9f
                                                            ),
                                                            RoundedCornerShape(
                                                                8.dp
                                                            )
                                                        )
                                            ) {

                                                Icon(
                                                    imageVector =
                                                        Icons.Default.Edit,
                                                    contentDescription =
                                                        "Modifica",
                                                    tint =
                                                        TravelBlue,
                                                    modifier =
                                                        Modifier.size(
                                                            16.dp
                                                        )
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    offerteViewModel
                                                        .eliminaItinerario(
                                                            item.id
                                                        )
                                                },
                                                modifier =
                                                    Modifier
                                                        .size(
                                                            34.dp
                                                        )
                                                        .background(
                                                            Color.White.copy(
                                                                alpha =
                                                                    0.9f
                                                            ),
                                                            RoundedCornerShape(
                                                                8.dp
                                                            )
                                                        )
                                            ) {

                                                Icon(
                                                    imageVector =
                                                        Icons.Default.Delete,
                                                    contentDescription =
                                                        "Elimina",
                                                    tint =
                                                        Color(
                                                            0xFFDC2626
                                                        ),
                                                    modifier =
                                                        Modifier.size(
                                                            16.dp
                                                        )
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    14.dp
                                                ),
                                        horizontalArrangement =
                                            Arrangement
                                                .SpaceBetween,
                                        verticalAlignment =
                                            Alignment
                                                .CenterVertically
                                    ) {

                                        Column(
                                            modifier =
                                                Modifier
                                                    .weight(
                                                        1f
                                                    )
                                        ) {

                                            Text(
                                                text =
                                                    item.titolo,
                                                fontWeight =
                                                    FontWeight.Bold,
                                                fontSize =
                                                    15.sp,
                                                color =
                                                    TravelTextDark
                                            )

                                            Text(
                                                text =
                                                    "${item.durataGiorni ?: 1} giorni • ${item.destinazionePrincipale ?: ""}",
                                                color =
                                                    TravelTextMuted,
                                                fontSize =
                                                    12.sp
                                            )
                                        }

                                        Text(
                                            text =
                                                "€${item.prezzoBase ?: "0"}",
                                            fontWeight =
                                                FontWeight.Bold,
                                            fontSize =
                                                16.sp,
                                            color =
                                                TravelBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            } else {

                if (
                    uiState.attivita.isEmpty()
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                "Nessuna attività creata. Creane una!",
                            color =
                                TravelTextMuted
                        )
                    }

                } else {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                14.dp
                            )
                    ) {

                        items(
                            uiState.attivita,
                            key = { it.id }
                        ) { item ->

                            Card(
                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),
                                colors =
                                    CardDefaults
                                        .cardColors(
                                            containerColor =
                                                TravelSurface
                                        ),
                                elevation =
                                    CardDefaults
                                        .cardElevation(
                                            2.dp
                                        ),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {

                                Column {

                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(
                                                    130.dp
                                                )
                                                .background(
                                                    Color(
                                                        0xFFE2E8F0
                                                    )
                                                )
                                    ) {

                                        Row(
                                            modifier =
                                                Modifier
                                                    .align(
                                                        Alignment.TopEnd
                                                    )
                                                    .padding(
                                                        8.dp
                                                    ),
                                            horizontalArrangement =
                                                Arrangement
                                                    .spacedBy(
                                                        6.dp
                                                    )
                                        ) {

                                            IconButton(
                                                onClick = {
                                                    onModificaAttivita(
                                                        item
                                                    )
                                                },
                                                modifier =
                                                    Modifier
                                                        .size(
                                                            34.dp
                                                        )
                                                        .background(
                                                            Color.White.copy(
                                                                alpha =
                                                                    0.9f
                                                            ),
                                                            RoundedCornerShape(
                                                                8.dp
                                                            )
                                                        )
                                            ) {

                                                Icon(
                                                    imageVector =
                                                        Icons.Default.Edit,
                                                    contentDescription =
                                                        "Modifica",
                                                    tint =
                                                        TravelBlue,
                                                    modifier =
                                                        Modifier.size(
                                                            16.dp
                                                        )
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    offerteViewModel
                                                        .eliminaAttivita(
                                                            item.id
                                                        )
                                                },
                                                modifier =
                                                    Modifier
                                                        .size(
                                                            34.dp
                                                        )
                                                        .background(
                                                            Color.White.copy(
                                                                alpha =
                                                                    0.9f
                                                            ),
                                                            RoundedCornerShape(
                                                                8.dp
                                                            )
                                                        )
                                            ) {

                                                Icon(
                                                    imageVector =
                                                        Icons.Default.Delete,
                                                    contentDescription =
                                                        "Elimina",
                                                    tint =
                                                        Color(
                                                            0xFFDC2626
                                                        ),
                                                    modifier =
                                                        Modifier.size(
                                                            16.dp
                                                        )
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    14.dp
                                                ),
                                        horizontalArrangement =
                                            Arrangement
                                                .SpaceBetween,
                                        verticalAlignment =
                                            Alignment
                                                .CenterVertically
                                    ) {

                                        Column(
                                            modifier =
                                                Modifier
                                                    .weight(
                                                        1f
                                                    )
                                        ) {

                                            Text(
                                                text =
                                                    item.titolo,
                                                fontWeight =
                                                    FontWeight.Bold,
                                                fontSize =
                                                    15.sp,
                                                color =
                                                    TravelTextDark
                                            )

                                            Text(
                                                text =
                                                    "${item.durataMinuti?.let { it / 60 } ?: 1}h • ${item.luogo ?: ""}",
                                                color =
                                                    TravelTextMuted,
                                                fontSize =
                                                    12.sp
                                            )
                                        }

                                        Text(
                                            text =
                                                "€${item.prezzo ?: "0"}",
                                            fontWeight =
                                                FontWeight.Bold,
                                            fontSize =
                                                16.sp,
                                            color =
                                                TravelOrange
                                        )
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