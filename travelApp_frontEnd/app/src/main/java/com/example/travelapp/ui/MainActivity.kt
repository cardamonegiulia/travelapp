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
import com.example.travelapp.ui.prenotazioni.BookingUiState
import com.example.travelapp.ui.prenotazioni.PrenotazionePasso1Screen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.screens.ProfileTab
import com.example.travelapp.ui.screens.ProfileUiState

// Unica Activity dell'app: per ora mostra direttamente la schermata Profilo.
// Quando arriveranno le altre schermate, qui andrà il NavHost (o il FragmentContainer)
// e la bottom navigation passerà a pilotare la navigazione vera.
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PrenotazionePasso1Screen(
                    uiState = BookingUiState(),
                    onIncrementa = {},
                    onDecrementa = {},
                    onContinua = {}
                )
            }
        }
    }
}

/**
 * Collega [ProfileScreen] a una sorgente di stato.
 *
 * Lo stato è tenuto in memoria e i dati dell'utente sono ancora segnaposto:
 * il punto di innesto naturale è `ProfiloViewModel`, che dovrà esporre un
 * `ProfileUiState` alimentato da `UtenteRepository`.
 */
@Composable
private fun ProfileRoute(onBack: () -> Unit) {
    var state by remember {
        mutableStateOf(
            ProfileUiState(
                name = "Mario Rossi",
                email = "mario@example.it",
                avatarUrl = null,
                isDarkModeEnabled = false,
                selectedTab = ProfileTab.PROFILE
            )
        )
    }

    ProfileScreen(
        state = state,
        onBack = onBack,
        // TODO: agganciare le destinazioni quando esisterà la navigazione.
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
