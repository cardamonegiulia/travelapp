package com.example.travelapp.ui.navigation

import android.widget.Toast
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.catalog.AdminDashboardScreen
import com.example.travelapp.ui.catalog.AttivitaDetailScreen
import com.example.travelapp.ui.catalog.CreaAttivitaScreen
import com.example.travelapp.ui.catalog.CreaItinerarioScreen
import com.example.travelapp.ui.catalog.GestioneUtentiAdminScreen
import com.example.travelapp.ui.catalog.ItinerarioDetailScreen
import com.example.travelapp.ui.catalog.OfferteManagementScreen
import com.example.travelapp.ui.catalog.OrganizzatoreHomeScreen
import com.example.travelapp.ui.components.AppBottomBar
import com.example.travelapp.ui.pagamenti.PaymentsScreen
import com.example.travelapp.ui.pagamenti.PaymentsViewModel
import com.example.travelapp.ui.prenotazioni.BookingsViewModel
import com.example.travelapp.ui.prenotazioni.PrenotazioneDettaglioScreen
import com.example.travelapp.ui.prenotazioni.PrenotazionePasso1Screen
import com.example.travelapp.ui.prenotazioni.PrenotazionePasso2Screen
import com.example.travelapp.ui.prenotazioni.PrenotazioneSuccessoScreen
import com.example.travelapp.ui.prenotazioni.PrenotazioniViewModel
import com.example.travelapp.ui.prenotazioni.PrenotazioniViewModelFactory
import com.example.travelapp.ui.preferiti.PreferitiViewModel
import com.example.travelapp.ui.profilo.ProfiloViewModel
import com.example.travelapp.ui.screens.BookingsScreen
import com.example.travelapp.ui.screens.ExploreScreen
import com.example.travelapp.ui.screens.FavoritesScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.theme.BackgroundLavender

object CatalogRoutes {
    const val ADMIN_HOME = "catalog/admin_home"
    const val ORGANIZZATORE_HOME = "catalog/organizzatore_home"
    const val CREA_ITINERARIO = "catalog/crea_itinerario"
    const val CREA_ATTIVITA = "catalog/crea_attivita"
    const val MODIFICA_ITINERARIO = "catalog/modifica_itinerario"
    const val MODIFICA_ATTIVITA = "catalog/modifica_attivita"
    const val LE_MIE_OFFERTE = "catalog/le_mie_offerte"
    const val OFFERTE_ADMIN = "catalog/offerte_admin"
    const val GESTIONE_UTENTI_ADMIN = "catalog/gestione_utenti_admin"
    const val DETTAGLIO_ITINERARIO = "catalog/dettaglio_itinerario"
    const val DETTAGLIO_ATTIVITA = "catalog/dettaglio_attivita"
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    profiloViewModel: ProfiloViewModel = viewModel(),
    ruoloIniziale: String? = null,
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val profiloState by profiloViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        profiloViewModel.caricaProfilo()
    }

    val onBack: () -> Unit = {
        if (!navController.popBackStack()) {
            onExitApp()
        }
    }

    val ruoloEffettivo = when {
        profiloState.ruolo.isNotBlank() && profiloState.ruolo != "VIAGGIATORE" -> profiloState.ruolo.uppercase()
        !ruoloIniziale.isNullOrBlank() -> ruoloIniziale.uppercase()
        else -> profiloState.ruolo.uppercase()
    }

    val isAdmin = ruoloEffettivo.contains("ADMIN")
    val isOrganizzatore = ruoloEffettivo.contains("ORGANIZZATORE")

    var itinerarioSelezionato by remember { mutableStateOf<Itinerario?>(null) }
    var attivitaSelezionata by remember { mutableStateOf<SingolaAttivita?>(null) }
    var itinerarioInModifica by remember { mutableStateOf<Itinerario?>(null) }
    var attivitaInModifica by remember { mutableStateOf<SingolaAttivita?>(null) }

    LaunchedEffect(isAdmin, isOrganizzatore) {
        when {
            isAdmin -> {
                navController.navigate(CatalogRoutes.ADMIN_HOME) {
                    popUpTo(AppDestination.Explore.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            isOrganizzatore -> {
                navController.navigate(CatalogRoutes.ORGANIZZATORE_HOME) {
                    popUpTo(AppDestination.Explore.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBookingWizard = currentRoute in setOf(
        AppDestination.BookingStep1.route,
        AppDestination.BookingStep2.route,
        AppDestination.BookingSuccess.route
    )

    val isCatalogAdminOrOrg = currentRoute in setOf(
        CatalogRoutes.ADMIN_HOME,
        CatalogRoutes.ORGANIZZATORE_HOME,
        CatalogRoutes.CREA_ITINERARIO,
        CatalogRoutes.CREA_ATTIVITA,
        CatalogRoutes.MODIFICA_ITINERARIO,
        CatalogRoutes.MODIFICA_ATTIVITA,
        CatalogRoutes.LE_MIE_OFFERTE,
        CatalogRoutes.OFFERTE_ADMIN,
        CatalogRoutes.GESTIONE_UTENTI_ADMIN
    )

    val mostraBottomBar = !isAdmin && !isOrganizzatore && !isBookingWizard && !isCatalogAdminOrOrg

    val startDest = when {
        isAdmin -> CatalogRoutes.ADMIN_HOME
        isOrganizzatore -> CatalogRoutes.ORGANIZZATORE_HOME
        else -> AppDestination.Explore.route
    }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        bottomBar = {
            if (mostraBottomBar) {
                AppBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(CatalogRoutes.ADMIN_HOME) {
                AdminDashboardScreen(
                    onVaiOfferte = { navController.navigate(CatalogRoutes.OFFERTE_ADMIN) },
                    onVaiUtenti = { navController.navigate(CatalogRoutes.GESTIONE_UTENTI_ADMIN) },
                    onVaiProfilo = { navController.navigate(AppDestination.Profile.route) },
                    onLogout = { onExitApp() }
                )
            }

            composable(CatalogRoutes.ORGANIZZATORE_HOME) {
                OrganizzatoreHomeScreen(
                    onCreaItinerario = { navController.navigate(CatalogRoutes.CREA_ITINERARIO) },
                    onCreaAttivita = { navController.navigate(CatalogRoutes.CREA_ATTIVITA) },
                    onModificaItinerario = { item ->
                        itinerarioInModifica = item
                        navController.navigate(CatalogRoutes.MODIFICA_ITINERARIO)
                    },
                    onModificaAttivita = { item ->
                        attivitaInModifica = item
                        navController.navigate(CatalogRoutes.MODIFICA_ATTIVITA)
                    },
                    onVaiProfilo = { navController.navigate(AppDestination.Profile.route) },
                    onLogout = { onExitApp() }
                )
            }

            composable(AppDestination.Explore.route) {
                ExploreScreen(
                    onItinerarioClick = { itinerario ->
                        itinerarioSelezionato = itinerario
                        navController.navigate(CatalogRoutes.DETTAGLIO_ITINERARIO)
                    },
                    onAttivitaClick = { attivita ->
                        attivitaSelezionata = attivita
                        navController.navigate(CatalogRoutes.DETTAGLIO_ATTIVITA)
                    }
                )
            }

            composable(AppDestination.Bookings.route) {
                BookingsRoute()
            }

            composable(AppDestination.Payments.route) {
                PaymentsRoute(onBack = { navController.popBackStack() })
            }

            composable(AppDestination.Favorites.route) {
                FavoritesRoute(
                    onBack = onBack,
                    onItinerarioClick = { itinerario ->
                        itinerarioSelezionato = itinerario
                        navController.navigate(CatalogRoutes.DETTAGLIO_ITINERARIO)
                    }
                )
            }

            composable(AppDestination.Profile.route) {
                ProfileRoute(
                    onBack = onBack,
                    onNavigateTo = { destination -> navController.navigate(destination.route) },
                    onLogout = { onExitApp() },
                    viewModel = profiloViewModel
                )
            }

            composable(CatalogRoutes.DETTAGLIO_ITINERARIO) {
                itinerarioSelezionato?.let { item ->
                    ItinerarioDetailScreen(
                        itinerario = item,
                        onBack = onBack,
                        onPrenota = { disponibilitaId ->
                            Toast.makeText(context, "Slot disponibilità #$disponibilitaId selezionato", Toast.LENGTH_SHORT).show()
                            navController.navigate(AppDestination.Bookings.route)
                        }
                    )
                }
            }

            composable(CatalogRoutes.DETTAGLIO_ATTIVITA) {
                attivitaSelezionata?.let { item ->
                    AttivitaDetailScreen(
                        attivita = item,
                        onBack = onBack,
                        onPrenota = { sessioneId ->
                            Toast.makeText(context, "Slot sessione #$sessioneId selezionato", Toast.LENGTH_SHORT).show()
                            navController.navigate(AppDestination.Bookings.route)
                        }
                    )
                }
            }

            composable(CatalogRoutes.CREA_ITINERARIO) {
                CreaItinerarioScreen(onBack = onBack)
            }

            composable(CatalogRoutes.MODIFICA_ITINERARIO) {
                CreaItinerarioScreen(
                    itinerarioDaModificare = itinerarioInModifica,
                    onBack = onBack
                )
            }

            composable(CatalogRoutes.CREA_ATTIVITA) {
                CreaAttivitaScreen(onBack = onBack)
            }

            composable(CatalogRoutes.MODIFICA_ATTIVITA) {
                CreaAttivitaScreen(
                    attivitaDaModificare = attivitaInModifica,
                    onBack = onBack
                )
            }

            composable(CatalogRoutes.LE_MIE_OFFERTE) {
                OfferteManagementScreen(
                    isAdmin = false,
                    onBack = onBack,
                    onModificaItinerario = { item ->
                        itinerarioInModifica = item
                        navController.navigate(CatalogRoutes.MODIFICA_ITINERARIO)
                    },
                    onModificaAttivita = { item ->
                        attivitaInModifica = item
                        navController.navigate(CatalogRoutes.MODIFICA_ATTIVITA)
                    }
                )
            }

            composable(CatalogRoutes.OFFERTE_ADMIN) {
                OfferteManagementScreen(
                    isAdmin = true,
                    onBack = onBack
                )
            }

            composable(CatalogRoutes.GESTIONE_UTENTI_ADMIN) {
                GestioneUtentiAdminScreen(onBack = onBack)
            }

            navigation(
                startDestination = AppDestination.BookingStep1.route,
                route = "booking_graph"
            ) {
                composable(AppDestination.BookingStep1.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("booking_graph")
                    }
                    val bookingContext = LocalContext.current
                    val bookingViewModel: PrenotazioniViewModel = viewModel(
                        viewModelStoreOwner = parentEntry,
                        factory = PrenotazioniViewModelFactory(bookingContext)
                    )
                    val state by bookingViewModel.uiState.collectAsState()

                    LaunchedEffect(state.prenotazioneCreata) {
                        if (state.prenotazioneCreata != null) {
                            navController.navigate(AppDestination.BookingStep2.route)
                        }
                    }

                    PrenotazionePasso1Screen(
                        uiState = state,
                        extraDisponibili = emptyList(),
                        onIncrementa = bookingViewModel::incrementaPartecipanti,
                        onDecrementa = bookingViewModel::decrementaPartecipanti,
                        onToggleExtra = bookingViewModel::toggleExtra,
                        onContinua = {}
                    )
                }

                composable(AppDestination.BookingStep2.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("booking_graph")
                    }
                    val bookingContext = LocalContext.current
                    val bookingViewModel: PrenotazioniViewModel = viewModel(
                        viewModelStoreOwner = parentEntry,
                        factory = PrenotazioniViewModelFactory(bookingContext)
                    )
                    val state by bookingViewModel.uiState.collectAsState()

                    LaunchedEffect(state.pagamentoCompletato) {
                        if (state.pagamentoCompletato != null) {
                            navController.navigate(AppDestination.BookingSuccess.route)
                        }
                    }

                    PrenotazionePasso2Screen(
                        uiState = state,
                        onMetodoPagamentoSelezionato = bookingViewModel::selezionaMetodoPagamento,
                        onConfermaEPaga = {
                            bookingViewModel.pagaPrenotazione()
                        }
                    )
                }

                composable(AppDestination.BookingSuccess.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("booking_graph")
                    }
                    val bookingContext = LocalContext.current
                    val bookingViewModel: PrenotazioniViewModel = viewModel(
                        viewModelStoreOwner = parentEntry,
                        factory = PrenotazioniViewModelFactory(bookingContext)
                    )
                    val state by bookingViewModel.uiState.collectAsState()

                    PrenotazioneSuccessoScreen(
                        uiState = state,
                        onFine = {
                            bookingViewModel.resetBooking()
                            navController.navigate(AppDestination.Bookings.route) {
                                popUpTo("booking_graph") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesRoute(
    onBack: () -> Unit,
    onItinerarioClick: (Itinerario) -> Unit,
    viewModel: PreferitiViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    FavoritesScreen(
        state = state,
        onBack = onBack,
        onSectionChange = viewModel::cambiaSezione,
        onOpenList = viewModel::apriLista,
        onCloseList = viewModel::chiudiLista,
        onCreateList = viewModel::creaLista,
        onChangeVisibility = viewModel::cambiaVisibilita,
        onDeleteList = viewModel::eliminaLista,
        onRemoveTrip = viewModel::rimuoviItinerario,
        onShareWithEmail = viewModel::condividiConEmail,
        onRevokeShare = viewModel::revocaCondivisione,
        onTripClick = { itinerarioId ->
            state.listaAperta
                ?.itinerari
                ?.firstOrNull { itinerario -> itinerario.id == itinerarioId }
                ?.let(onItinerarioClick)
        },
        onMessageShown = viewModel::messaggioMostrato
    )
}

@Composable
private fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    onLogout: () -> Unit = {},
    viewModel: ProfiloViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

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
        onPaymentsClick = { onNavigateTo(AppDestination.Payments) },
        onReviewsClick = {},
        onAddProfilePhoto = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onPhotoMessageShown = viewModel::messaggioMostrato,
        onToggleDarkMode = viewModel::cambiaTemaScuro,
        onChangePassword = {},
        onLogout = onLogout
    )
}

@Composable
private fun BookingsRoute(
    viewModel: BookingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val prenotazioneSelezionata = state.prenotazioneSelezionata

    if (prenotazioneSelezionata != null) {
        PrenotazioneDettaglioScreen(
            prenotazione = prenotazioneSelezionata,
            onBack = { viewModel.chiudiDettaglio() },
            onAnnulla = { viewModel.annullaPrenotazione() },
            isLoading = state.isLoading
        )
    } else {
        BookingsScreen(
            prenotazioni = state.prenotazioni,
            isLoading = state.isLoading,
            errore = state.errore,
            onRiprova = { viewModel.caricaPrenotazioni() },
            onPrenotazioneClick = { viewModel.selezionaPrenotazione(it) }
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
        onRiprova = { viewModel.caricaPagamenti() },
        onBack = onBack
    )
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "App"
)
@Composable
private fun AppNavGraphPreview() {
    MaterialTheme {
        AppNavGraph()
    }
}