package com.example.travelapp.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.ui.components.AppBottomBar
import com.example.travelapp.ui.pagamenti.PaymentsScreen
import com.example.travelapp.ui.pagamenti.PaymentsViewModel
import com.example.travelapp.ui.prenotazioni.BookingsViewModel
import com.example.travelapp.ui.prenotazioni.PrenotazioneDettaglioScreen
import com.example.travelapp.ui.profilo.ProfiloViewModel
import com.example.travelapp.ui.screens.BookingsScreen
import com.example.travelapp.ui.screens.ExploreScreen
import com.example.travelapp.ui.screens.FavoritesScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.prenotazioni.PrenotazionePasso1Screen
import com.example.travelapp.ui.prenotazioni.PrenotazionePasso2Screen
import com.example.travelapp.ui.prenotazioni.PrenotazioneSuccessoScreen
import com.example.travelapp.ui.prenotazioni.PrenotazioniViewModel
import com.example.travelapp.ui.prenotazioni.PrenotazioniViewModelFactory
import androidx.navigation.compose.navigation
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mostraBottomBar = currentRoute !in setOf(
        AppDestination.BookingStep1.route,
        AppDestination.BookingStep2.route,
        AppDestination.BookingSuccess.route
    )

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        bottomBar = { if (mostraBottomBar) { AppBottomBar(navController = navController) } }
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
                BookingsRoute()
            }
            composable(AppDestination.Payments.route) {
                PaymentsRoute(
                    onBack = {
                        navController.popBackStack()
                    }
                )
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
            navigation(
                startDestination = AppDestination.BookingStep1.route,
                route = "booking_graph"
            ) {

                composable(AppDestination.BookingStep1.route) { backStackEntry ->

                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("booking_graph")
                    }

                    val context = LocalContext.current

                    val bookingViewModel: PrenotazioniViewModel = viewModel(
                        viewModelStoreOwner = parentEntry,
                        factory = PrenotazioniViewModelFactory(context)
                    )

                    val state by bookingViewModel.uiState.collectAsState()

                    LaunchedEffect(state.prenotazioneCreata) {
                        if (state.prenotazioneCreata != null) {
                            navController.navigate(
                                AppDestination.BookingStep2.route
                            )
                        }
                    }

                    PrenotazionePasso1Screen(
                        uiState = state,
                        extraDisponibili = emptyList(),
                        onIncrementa = bookingViewModel::incrementaPartecipanti,
                        onDecrementa = bookingViewModel::decrementaPartecipanti,
                        onToggleExtra = bookingViewModel::toggleExtra,
                        onContinua = {
                            // qui chiameremo bookingViewModel.creaPrenotazione(...)
                            // quando avremo l'ID reale della disponibilità/sessione
                        }
                    )
                }

                composable(AppDestination.BookingStep2.route) { backStackEntry ->

                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("booking_graph")
                    }

                    val context = LocalContext.current

                    val bookingViewModel: PrenotazioniViewModel = viewModel(
                        viewModelStoreOwner = parentEntry,
                        factory = PrenotazioniViewModelFactory(context)
                    )

                    val state by bookingViewModel.uiState.collectAsState()

                    LaunchedEffect(state.pagamentoCompletato) {
                        if (state.pagamentoCompletato != null) {
                            navController.navigate(
                                AppDestination.BookingSuccess.route
                            )
                        }
                    }

                    PrenotazionePasso2Screen(
                        uiState = state,
                        onMetodoPagamentoSelezionato =
                            bookingViewModel::selezionaMetodoPagamento,
                        onConfermaEPaga = {
                            bookingViewModel.pagaPrenotazione()
                        }
                    )
                }

                composable(AppDestination.BookingSuccess.route) { backStackEntry ->

                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("booking_graph")
                    }

                    val context = LocalContext.current

                    val bookingViewModel: PrenotazioniViewModel = viewModel(
                        viewModelStoreOwner = parentEntry,
                        factory = PrenotazioniViewModelFactory(context)
                    )

                    val state by bookingViewModel.uiState.collectAsState()

                    PrenotazioneSuccessoScreen(
                        uiState = state,
                        onFine = {
                            bookingViewModel.resetBooking()

                            navController.navigate(
                                AppDestination.Bookings.route
                            ) {
                                popUpTo("booking_graph") {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
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
 * Collega [ProfileScreen] al suo [ProfiloViewModel], che tiene lo stato e parla col
 * backend.
 *
 * Lo stato vive nel ViewModel e non in un `remember` locale perche' il caricamento della
 * foto e' asincrono: con lo stato nella composizione una rotazione dello schermo lo
 * butterebbe via a meta' upload.
 */
@Composable
private fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    viewModel: ProfiloViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Photo picker di sistema: non richiede permessi, l'accesso all'immagine
    // scelta vale per la sessione corrente. Per questo il file va letto e
    // caricato subito, non conservato per dopo.
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.cambiaFotoProfilo(uri)
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
        onPhotoMessageShown = viewModel::messaggioMostrato,
        // TODO: agganciare le destinazioni mancanti quando le schermate esisteranno.
        onPaymentsClick = { onNavigateTo(AppDestination.Payments) },
        onReviewsClick = {},
        onToggleDarkMode = viewModel::cambiaTemaScuro,
        onChangePassword = {},
        onLogout = {}
    )
}

@Composable
private fun BookingsRoute(
    viewModel: BookingsViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()

    val prenotazioneSelezionata =
        state.prenotazioneSelezionata

    if (prenotazioneSelezionata != null) {

        PrenotazioneDettaglioScreen(
            prenotazione = prenotazioneSelezionata,
            onBack = {
                viewModel.chiudiDettaglio()
            },
            onAnnulla = {
                viewModel.annullaPrenotazione()
            },
            isLoading = state.isLoading
        )

    } else {

        BookingsScreen(
            prenotazioni = state.prenotazioni,
            isLoading = state.isLoading,
            errore = state.errore,
            onRiprova = {
                viewModel.caricaPrenotazioni()
            },
            onPrenotazioneClick = {
                viewModel.selezionaPrenotazione(it)
            }
        )
    }
}

@Composable
private fun PaymentsRoute(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsState()

    PaymentsScreen(
        pagamenti = state.pagamenti,
        isLoading = state.isLoading,
        errore = state.errore,
        onRiprova = {
            viewModel.caricaPagamenti()
        },
        onBack = onBack
    )
}



@Preview(showBackground = true, showSystemUi = true, name = "App")
@Composable
private fun AppNavGraphPreview() {
    MaterialTheme {
        AppNavGraph()
    }
}
