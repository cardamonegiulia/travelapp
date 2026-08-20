package com.example.travelapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.catalog.AttivitaDetailScreen
import com.example.travelapp.ui.catalog.CatalogScreen
import com.example.travelapp.ui.catalog.CatalogViewModel
import com.example.travelapp.ui.catalog.ItinerarioDetailScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.screens.ProfileTab
import com.example.travelapp.ui.screens.ProfileUiState
import java.math.BigDecimal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppRoute(
                    onBack = { finish() },
                    showToast = { messaggio ->
                        Toast.makeText(this, messaggio, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun MainAppRoute(
    onBack: () -> Unit,
    showToast: (String) -> Unit,
    catalogViewModel: CatalogViewModel = viewModel()
) {
    var state by remember {
        mutableStateOf(
            ProfileUiState(
                name = "Mario Rossi",
                email = "mario@example.it",
                avatarUrl = null,
                isDarkModeEnabled = false,
                selectedTab = ProfileTab.EXPLORE
            )
        )
    }

    val catalogState by catalogViewModel.uiState.collectAsState()

    val itinerarioMock = remember {
        Itinerario(
            id = 1L,
            organizzatoreId = 1L,
            titolo = "Tour Costiera Amalfitana e Capri",
            descrizione = "Un viaggio indimenticabile tra panorami mozzafiato, mare cristallino, escursioni in barca e degustazioni locali.",
            destinazionePrincipale = "Costiera Amalfitana",
            prezzoBase = BigDecimal("450.00"),
            durataGiorni = 5,
            maxPartecipanti = 15,
            stato = "ATTIVO"
        )
    }

    // Inizializzato direttamente con i dati mock per il test visivo
    var selectedItinerario by remember { mutableStateOf<Itinerario?>(itinerarioMock) }
    var selectedAttivita by remember { mutableStateOf<SingolaAttivita?>(null) }

    when {
        selectedItinerario != null -> {
            ItinerarioDetailScreen(
                itinerario = selectedItinerario!!,
                onBack = { selectedItinerario = null },
                onPrenota = { id ->
                    showToast("Prenotazione itinerario #$id inviata con successo!")
                }
            )
        }
        selectedAttivita != null -> {
            AttivitaDetailScreen(
                attivita = selectedAttivita!!,
                onBack = { selectedAttivita = null },
                onPrenota = { id ->
                    showToast("Prenotazione attività #$id inviata con successo!")
                }
            )
        }
        state.selectedTab == ProfileTab.EXPLORE -> {
            CatalogScreen(
                viewModel = catalogViewModel,
                onItinerarioClick = { id ->
                    selectedItinerario = catalogState.itinerari.find { it.id == id }
                },
                onAttivitaClick = { id ->
                    selectedAttivita = catalogState.attivita.find { it.id == id }
                }
            )
        }
        state.selectedTab == ProfileTab.PROFILE -> {
            ProfileScreen(
                state = state,
                onBack = onBack,
                onEditProfile = {},
                onBookingsClick = {},
                onPaymentsClick = {},
                onFavoritesClick = {},
                onReviewsClick = {},
                onToggleDarkMode = { enabled -> state = state.copy(isDarkModeEnabled = enabled) },
                onChangePassword = {},
                onLogout = {},
                onNavigate = { tab -> state = state.copy(selectedTab = tab) }
            )
        }
        else -> {
            CatalogScreen(viewModel = catalogViewModel)
        }
    }
}