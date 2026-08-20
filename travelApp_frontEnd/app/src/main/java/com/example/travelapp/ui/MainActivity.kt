package com.example.travelapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.travelapp.ui.catalog.CatalogScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.screens.ProfileTab
import com.example.travelapp.ui.screens.ProfileUiState

// Unica Activity dell'app: pilota la navigazione tra le schermate tramite il tab attivo.
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppRoute(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun MainAppRoute(onBack: () -> Unit) {
    // Inizializziamo su EXPLORE per mostrare il catalogo all'avvio
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

    when (state.selectedTab) {
        ProfileTab.EXPLORE -> {
            CatalogScreen(
                onItinerarioClick = { /* Gestione click dettaglio */ },
                onAttivitaClick = { /* Gestione click dettaglio */ }
            )
        }
        ProfileTab.PROFILE -> {
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
            // Selezionando altri tab non ancora implementati
            CatalogScreen()
        }
    }
}
