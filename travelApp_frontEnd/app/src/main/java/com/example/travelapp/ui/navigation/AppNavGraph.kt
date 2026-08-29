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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.travelapp.data.remote.TokenManager
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
import com.example.travelapp.ui.preferiti.PreferitiViewModel
import com.example.travelapp.ui.prenotazioni.BookingsViewModel
import com.example.travelapp.ui.prenotazioni.PrenotazioneDettaglioScreen
import com.example.travelapp.ui.prenotazioni.PrenotazionePasso1Screen
import com.example.travelapp.ui.prenotazioni.PrenotazionePasso2Screen
import com.example.travelapp.ui.prenotazioni.PrenotazioneSuccessoScreen
import com.example.travelapp.ui.prenotazioni.PrenotazioniViewModel
import com.example.travelapp.ui.prenotazioni.PrenotazioniViewModelFactory
import com.example.travelapp.ui.profilo.ProfiloViewModel
import com.example.travelapp.ui.screens.BookingsScreen
import com.example.travelapp.ui.screens.CambiaPasswordScreen
import com.example.travelapp.ui.screens.ExploreScreen
import com.example.travelapp.ui.screens.FavoritesScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.theme.BackgroundLavender
import kotlinx.coroutines.launch

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


object ProfiloRoutes {

    const val CAMBIA_PASSWORD =
        "profilo/cambia_password"
}


@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    profiloViewModel: ProfiloViewModel = viewModel(),
    onExitApp: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDarkModeChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    val bookingViewModel: PrenotazioniViewModel =
        viewModel(
            factory = PrenotazioniViewModelFactory(context)
        )

    val coroutineScope =
        rememberCoroutineScope()

    // Logout condiviso da Admin, Organizzatore e Profilo.
    val eseguiLogout: () -> Unit = {
        coroutineScope.launch {
            TokenManager.cancellaToken(context)
            onLogout()
        }
    }

    /*
     * Utilizziamo la stessa istanza del ProfiloViewModel
     * sia per il profilo sia per determinare il ruolo
     * dell'utente e quindi la relativa home.
     */
    val profiloState by
    profiloViewModel.state.collectAsState()

    val onBack: () -> Unit = {
        if (!navController.popBackStack()) {
            onExitApp()
        }
    }

    // Inoltra il tema a MainActivity. Aspetta il profilo vero (id != null) per non
    // sovrascrivere per un istante il tema di sistema col placeholder iniziale.
    LaunchedEffect(profiloState.isDarkModeEnabled, profiloState.id) {
        if (profiloState.id != null) {
            onDarkModeChanged(profiloState.isDarkModeEnabled)
        }
    }

    /*
     * Determinazione del ruolo.
     */
    val ruoloStr =
        profiloState.ruolo
            ?.toString()
            ?.uppercase()
            ?: ""

    val ruoloEffettivo = when {
        ruoloProfilo.isNotBlank() &&
                ruoloProfilo != "VIAGGIATORE" &&
                ruoloProfilo != "NULL" -> {
            ruoloProfilo
        }
        !ruoloIniziale.isNullOrBlank() -> {
            ruoloIniziale.uppercase()
        }
        else -> {
            if (ruoloProfilo == "NULL") "" else ruoloProfilo
        }
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

    val startDestination = when {
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
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(CatalogRoutes.ADMIN_HOME) {
                AdminDashboardScreen(

                    onVaiOfferte = {

                        navController.navigate(
                            CatalogRoutes.OFFERTE_ADMIN
                        )
                    },

                    onVaiUtenti = {

                        navController.navigate(
                            CatalogRoutes.GESTIONE_UTENTI_ADMIN
                        )
                    },

                    onLogout = eseguiLogout
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

                    onLogout = eseguiLogout
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
                PaymentsRoute(
                    onBack = { navController.popBackStack() }
                )
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
                    viewModel = profiloViewModel,
                    onBack = onBack,
                    onNavigateTo = { destination ->
                        navController.navigate(destination.route)
                    },

                    onNavigateToRoute = { route ->

                        navController.navigate(
                            route
                        )
                    },

                    onLogout = eseguiLogout,

                    viewModel =
                    profiloViewModel
                )
            }


            composable(
                ProfiloRoutes.CAMBIA_PASSWORD
            ) {

                CambiaPasswordScreen(
                    onBack = onBack,
                    // il backend chiude tutte le sessioni: da qui si esce col logout, non onBack
                    onPasswordCambiata = eseguiLogout
                )
            }


            /*
             * ============================================================
             * DETTAGLI CATALOGO
             * ============================================================
             */

            composable(
                CatalogRoutes.DETTAGLIO_ITINERARIO
            ) {

                itinerarioSelezionato?.let { item ->
                    ItinerarioDetailScreen(
                        itinerario = item,
                        onBack = onBack,
                        onPrenota = { disponibilitaId ->
                            bookingViewModel.inizializzaBooking(
                                titolo = item.titolo,
                                luogo = item.destinazionePrincipale ?: "",
                                prezzoBaseUnitario = item.prezzoBase?.toDouble() ?: 0.0,
                                disponibilitaItinerarioId = disponibilitaId,
                                sessioneSingolaAttivitaId = null
                            )
                            navController.navigate(AppDestination.BookingStep1.route) {
                                launchSingleTop = true
                            }
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
                            bookingViewModel.inizializzaBooking(
                                titolo = item.titolo,
                                luogo = item.luogo ?: "",
                                prezzoBaseUnitario = item.prezzo?.toDouble() ?: 0.0,
                                disponibilitaItinerarioId = null,
                                sessioneSingolaAttivitaId = sessioneId
                            )
                            navController.navigate(AppDestination.BookingStep1.route) {
                                launchSingleTop = true
                            }
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
                composable(AppDestination.BookingStep1.route) {
                    LaunchedEffect(bookingState.prenotazioneCreata?.id) {
                        if (bookingState.prenotazioneCreata != null) {
                            navController.navigate(AppDestination.BookingStep2.route) {
                                popUpTo(AppDestination.BookingStep1.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }

                    PrenotazionePasso1Screen(
                        uiState = bookingState,
                        extraDisponibili = emptyList(),
                        onIncrementa = bookingViewModel::incrementaPartecipanti,
                        onDecrementa = bookingViewModel::decrementaPartecipanti,
                        onToggleExtra = bookingViewModel::toggleExtra,
                        onContinua = { bookingViewModel.creaPrenotazione() }
                    )
                }

                composable(AppDestination.BookingStep2.route) {
                    LaunchedEffect(bookingState.pagamentoCompletato) {
                        if (bookingState.pagamentoCompletato != null) {
                            navController.navigate(AppDestination.BookingSuccess.route) {
                                popUpTo(AppDestination.BookingStep2.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }

                    PrenotazionePasso2Screen(
                        uiState = bookingState,
                        onMetodoPagamentoSelezionato = bookingViewModel::selezionaMetodoPagamento,
                        onConfermaEPaga = { bookingViewModel.pagaPrenotazione() }
                    )
                }

                composable(AppDestination.BookingSuccess.route) {
                    PrenotazioneSuccessoScreen(
                        uiState = bookingState,
                        onFine = {
                            bookingViewModel.resetBooking()
                            navController.navigate(AppDestination.Bookings.route) {
                                popUpTo("booking_graph") {
                                    inclusive = true
                                }
                                launchSingleTop = true
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
                ?.firstOrNull { it.id == itinerarioId }
                ?.let(onItinerarioClick)
        },
        onMessageShown = viewModel::messaggioMostrato
    )
}

@Composable
private fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfiloViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.caricaProfilo()
    }

    val state by viewModel.state.collectAsState()

    val photoPicker =
        rememberLauncherForActivityResult(
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

        onToggleDarkMode =
        viewModel::cambiaTemaScuro,

        onChangePassword = {
            onNavigateToRoute(ProfiloRoutes.CAMBIA_PASSWORD)
        },

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