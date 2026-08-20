package com.example.travelapp.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.ui.components.AppBottomBar
import com.example.travelapp.ui.screens.BookingsScreen
import com.example.travelapp.ui.screens.ExploreScreen
import com.example.travelapp.ui.screens.FavoritesScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.screens.ProfileUiState
import com.example.travelapp.ui.screens.sampleFavoriteTrips
import com.example.travelapp.ui.theme.BackgroundLavender

/**
 * Struttura di navigazione dell'app: la bottom bar sta attorno al [NavHost],
 * quindi resta fissa mentre cambia solo il contenuto sopra di essa.
 *
 * [onExitApp] viene invocata quando non c'e' piu' nulla da cui tornare
 * indietro, cioe' su una delle sezioni principali.
 */
@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onExitApp: () -> Unit = {}
) {
    val onBack: () -> Unit = { if (!navController.popBackStack()) onExitApp() }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        bottomBar = { AppBottomBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.start.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(AppDestination.Explore.route) {
                ExploreScreen()
            }
            composable(AppDestination.Bookings.route) {
                BookingsScreen()
            }
            composable(AppDestination.Favorites.route) {
                FavoritesRoute(onBack = onBack)
            }
            composable(AppDestination.Profile.route) {
                ProfileRoute(
                    onBack = onBack,
                    onNavigateTo = { destination -> navController.navigate(destination.route) }
                )
            }
        }
    }
}

/**
 * Collega [FavoritesScreen] a una sorgente di stato.
 *
 * I viaggi sono ancora segnaposto: il punto di innesto naturale e' un
 * `FavoritesViewModel` alimentato da `ItinerarioRepository`.
 */
@Composable
private fun FavoritesRoute(onBack: () -> Unit) {
    var trips by remember { mutableStateOf(sampleFavoriteTrips) }

    FavoritesScreen(
        trips = trips,
        onBack = onBack,
        onToggleFavorite = { id ->
            trips = trips.map { trip ->
                if (trip.id == id) trip.copy(isFavorite = !trip.isFavorite) else trip
            }
        },
        onLoadMore = {},
        // TODO: navigare al dettaglio del viaggio quando la schermata esistera'.
        onTripClick = {}
    )
}

/**
 * Collega [ProfileScreen] a una sorgente di stato.
 *
 * I dati dell'utente sono ancora segnaposto: il punto di innesto naturale e'
 * `ProfiloViewModel`, che dovra' esporre un [ProfileUiState] alimentato da
 * `UtenteRepository`.
 */
@Composable
private fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit
) {
    var state by remember {
        mutableStateOf(
            ProfileUiState(
                name = "Mario Rossi",
                email = "mario@example.it",
                avatarUrl = null,
                isDarkModeEnabled = false
            )
        )
    }

    // Photo picker di sistema: non richiede permessi, l'accesso all'immagine
    // scelta vale per la sessione corrente.
    // TODO: caricare la foto sul backend quando l'endpoint sara' disponibile;
    // per ora resta solo nello stato in memoria.
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            state = state.copy(avatarUrl = uri.toString())
        }
    }

    ProfileScreen(
        state = state,
        onBack = onBack,
        onBookingsClick = { onNavigateTo(AppDestination.Bookings) },
        onFavoritesClick = { onNavigateTo(AppDestination.Favorites) },
        onAddProfilePhoto = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        // TODO: agganciare le destinazioni mancanti quando le schermate esisteranno.
        onPaymentsClick = {},
        onReviewsClick = {},
        onToggleDarkMode = { enabled -> state = state.copy(isDarkModeEnabled = enabled) },
        onChangePassword = {},
        onLogout = {}
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "App")
@Composable
private fun AppNavGraphPreview() {
    MaterialTheme {
        AppNavGraph()
    }
}
