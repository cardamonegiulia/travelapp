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
import com.example.travelapp.ui.screens.CambiaPasswordScreen
import com.example.travelapp.ui.screens.ExploreScreen
import com.example.travelapp.ui.screens.FavoritesScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.theme.BackgroundLavender
import kotlinx.coroutines.launch


object CatalogRoutes {

    const val ADMIN_HOME =
        "catalog/admin_home"

    const val ORGANIZZATORE_HOME =
        "catalog/organizzatore_home"

    const val CREA_ITINERARIO =
        "catalog/crea_itinerario"

    const val CREA_ATTIVITA =
        "catalog/crea_attivita"

    const val MODIFICA_ITINERARIO =
        "catalog/modifica_itinerario"

    const val MODIFICA_ATTIVITA =
        "catalog/modifica_attivita"

    const val LE_MIE_OFFERTE =
        "catalog/le_mie_offerte"

    const val OFFERTE_ADMIN =
        "catalog/offerte_admin"

    const val GESTIONE_UTENTI_ADMIN =
        "catalog/gestione_utenti_admin"

    const val DETTAGLIO_ITINERARIO =
        "catalog/dettaglio_itinerario"

    const val DETTAGLIO_ATTIVITA =
        "catalog/dettaglio_attivita"
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
    onLogout: () -> Unit = {}
) {

    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()

    /*
     * Cancella il token salvato e notifica il chiamante (che riporta
     * l'app alla schermata di login): condivisa da tutte le schermate
     * che espongono un pulsante di logout.
     */
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

    /*
     * Determinazione del ruolo.
     */
    val ruoloStr =
        profiloState.ruolo
            ?.toString()
            ?.uppercase()
            ?: ""

    val isAdmin =
        ruoloStr.contains("ADMIN")

    val isOrganizzatore =
        ruoloStr.contains("ORGANIZZATORE")


    /*
     * Elementi selezionati nel Catalog.
     */
    var itinerarioSelezionato by remember {
        mutableStateOf<Itinerario?>(null)
    }

    var attivitaSelezionata by remember {
        mutableStateOf<SingolaAttivita?>(null)
    }

    var itinerarioInModifica by remember {
        mutableStateOf<Itinerario?>(null)
    }

    var attivitaInModifica by remember {
        mutableStateOf<SingolaAttivita?>(null)
    }


    /*
     * Quando il ruolo viene caricato dal profilo,
     * Admin e Organizzatore vengono reindirizzati
     * verso la rispettiva home dedicata.
     */
    LaunchedEffect(profiloState.ruolo) {

        when {

            isAdmin -> {

                navController.navigate(
                    CatalogRoutes.ADMIN_HOME
                ) {

                    popUpTo(
                        AppDestination.Explore.route
                    ) {
                        inclusive = true
                    }

                    launchSingleTop = true
                }
            }

            isOrganizzatore -> {

                navController.navigate(
                    CatalogRoutes.ORGANIZZATORE_HOME
                ) {

                    popUpTo(
                        AppDestination.Explore.route
                    ) {
                        inclusive = true
                    }

                    launchSingleTop = true
                }
            }
        }
    }


    /*
     * La bottom bar:
     *
     * - non viene mostrata ad ADMIN;
     * - non viene mostrata a ORGANIZZATORE;
     * - viene nascosta durante il wizard Booking.
     */
    val navBackStackEntry by
    navController.currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry
            ?.destination
            ?.route

    val isBookingWizard =
        currentRoute in setOf(
            AppDestination.BookingStep1.route,
            AppDestination.BookingStep2.route,
            AppDestination.BookingSuccess.route
        )

    val mostraBottomBar =
        !isAdmin &&
                !isOrganizzatore &&
                !isBookingWizard


    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,

        bottomBar = {

            if (mostraBottomBar) {

                AppBottomBar(
                    navController = navController
                )
            }
        }
    ) { innerPadding ->


        NavHost(
            navController = navController,
            startDestination =
            AppDestination.Explore.route,

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {


            /*
             * ============================================================
             * ADMIN
             * ============================================================
             */

            composable(
                CatalogRoutes.ADMIN_HOME
            ) {

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


            /*
             * ============================================================
             * ORGANIZZATORE
             * ============================================================
             */

            composable(
                CatalogRoutes.ORGANIZZATORE_HOME
            ) {

                OrganizzatoreHomeScreen(

                    onCreaItinerario = {

                        navController.navigate(
                            CatalogRoutes.CREA_ITINERARIO
                        )
                    },

                    onCreaAttivita = {

                        navController.navigate(
                            CatalogRoutes.CREA_ATTIVITA
                        )
                    },

                    onModificaItinerario = { item ->

                        itinerarioInModifica = item

                        navController.navigate(
                            CatalogRoutes.MODIFICA_ITINERARIO
                        )
                    },

                    onModificaAttivita = { item ->

                        attivitaInModifica = item

                        navController.navigate(
                            CatalogRoutes.MODIFICA_ATTIVITA
                        )
                    },

                    onLogout = eseguiLogout
                )
            }


            /*
             * ============================================================
             * VIAGGIATORE / NAVIGAZIONE PRINCIPALE
             * ============================================================
             */

            composable(
                AppDestination.Explore.route
            ) {

                ExploreScreen(

                    onItinerarioClick = { itinerario ->

                        itinerarioSelezionato =
                            itinerario

                        navController.navigate(
                            CatalogRoutes.DETTAGLIO_ITINERARIO
                        )
                    },

                    onAttivitaClick = { attivita ->

                        attivitaSelezionata =
                            attivita

                        navController.navigate(
                            CatalogRoutes.DETTAGLIO_ATTIVITA
                        )
                    }
                )
            }


            /*
             * BOOKING LIST
             */

            composable(
                AppDestination.Bookings.route
            ) {

                BookingsRoute()
            }


            /*
             * PAGAMENTI
             */

            composable(
                AppDestination.Payments.route
            ) {

                PaymentsRoute(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }


            /*
             * PREFERITI
             */

            composable(
                AppDestination.Favorites.route
            ) {

                FavoritesRoute(
                    onBack = onBack,

                    onItinerarioClick = { itinerario ->

                        itinerarioSelezionato =
                            itinerario

                        navController.navigate(
                            CatalogRoutes.DETTAGLIO_ITINERARIO
                        )
                    }
                )
            }


            /*
             * PROFILO
             */

            composable(
                AppDestination.Profile.route
            ) {

                ProfileRoute(
                    onBack = onBack,

                    onNavigateTo = { destination ->

                        navController.navigate(
                            destination.route
                        )
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
                    // Il backend chiude tutte le sessioni al cambio riuscito:
                    // il token attuale non è più valido, quindi da qui si esce
                    // con lo stesso logout usato altrove, non con un semplice onBack.
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

                            Toast.makeText(
                                context,
                                "Slot disponibilità #$disponibilitaId selezionato",
                                Toast.LENGTH_SHORT
                            ).show()

                            /*
                             * TODO Booking:
                             * collegare disponibilitaId
                             * al booking_graph.
                             */
                            navController.navigate(
                                AppDestination.Bookings.route
                            )
                        }
                    )
                }
            }


            composable(
                CatalogRoutes.DETTAGLIO_ATTIVITA
            ) {

                attivitaSelezionata?.let { item ->

                    AttivitaDetailScreen(
                        attivita = item,
                        onBack = onBack,

                        onPrenota = { sessioneId ->

                            Toast.makeText(
                                context,
                                "Slot sessione #$sessioneId selezionato",
                                Toast.LENGTH_SHORT
                            ).show()

                            /*
                             * TODO Booking:
                             * collegare sessioneId
                             * al booking_graph.
                             */
                            navController.navigate(
                                AppDestination.Bookings.route
                            )
                        }
                    )
                }
            }


            /*
             * ============================================================
             * CREAZIONE / MODIFICA CATALOGO
             * ============================================================
             */

            composable(
                CatalogRoutes.CREA_ITINERARIO
            ) {

                CreaItinerarioScreen(
                    onBack = onBack
                )
            }


            composable(
                CatalogRoutes.MODIFICA_ITINERARIO
            ) {

                CreaItinerarioScreen(
                    itinerarioDaModificare =
                    itinerarioInModifica,

                    onBack = onBack
                )
            }


            composable(
                CatalogRoutes.CREA_ATTIVITA
            ) {

                CreaAttivitaScreen(
                    onBack = onBack
                )
            }


            composable(
                CatalogRoutes.MODIFICA_ATTIVITA
            ) {

                CreaAttivitaScreen(
                    attivitaDaModificare =
                    attivitaInModifica,

                    onBack = onBack
                )
            }


            /*
             * ============================================================
             * OFFERTE ORGANIZZATORE
             * ============================================================
             */

            composable(
                CatalogRoutes.LE_MIE_OFFERTE
            ) {

                OfferteManagementScreen(
                    isAdmin = false,
                    onBack = onBack,

                    onModificaItinerario = { item ->

                        itinerarioInModifica =
                            item

                        navController.navigate(
                            CatalogRoutes.MODIFICA_ITINERARIO
                        )
                    },

                    onModificaAttivita = { item ->

                        attivitaInModifica =
                            item

                        navController.navigate(
                            CatalogRoutes.MODIFICA_ATTIVITA
                        )
                    }
                )
            }


            /*
             * ============================================================
             * OFFERTE ADMIN
             * ============================================================
             */

            composable(
                CatalogRoutes.OFFERTE_ADMIN
            ) {

                OfferteManagementScreen(
                    isAdmin = true,
                    onBack = onBack
                )
            }


            /*
             * ============================================================
             * GESTIONE UTENTI ADMIN
             * ============================================================
             */

            composable(
                CatalogRoutes.GESTIONE_UTENTI_ADMIN
            ) {

                GestioneUtentiAdminScreen(
                    onBack = onBack
                )
            }


            /*
             * ============================================================
             * BOOKING WIZARD
             * ============================================================
             *
             * I tre step condividono la stessa istanza
             * di PrenotazioniViewModel tramite booking_graph.
             */

            navigation(
                startDestination =
                AppDestination.BookingStep1.route,

                route = "booking_graph"
            ) {


                /*
                 * STEP 1
                 */

                composable(
                    AppDestination.BookingStep1.route
                ) { backStackEntry ->

                    val parentEntry =
                        remember(backStackEntry) {

                            navController
                                .getBackStackEntry(
                                    "booking_graph"
                                )
                        }

                    val bookingContext =
                        LocalContext.current

                    val bookingViewModel:
                            PrenotazioniViewModel =
                        viewModel(
                            viewModelStoreOwner =
                            parentEntry,

                            factory =
                            PrenotazioniViewModelFactory(
                                bookingContext
                            )
                        )

                    val state by
                    bookingViewModel
                        .uiState
                        .collectAsState()


                    LaunchedEffect(
                        state.prenotazioneCreata
                    ) {

                        if (
                            state.prenotazioneCreata != null
                        ) {

                            navController.navigate(
                                AppDestination
                                    .BookingStep2
                                    .route
                            )
                        }
                    }


                    PrenotazionePasso1Screen(
                        uiState = state,

                        extraDisponibili =
                        emptyList(),

                        onIncrementa =
                        bookingViewModel::
                        incrementaPartecipanti,

                        onDecrementa =
                        bookingViewModel::
                        decrementaPartecipanti,

                        onToggleExtra =
                        bookingViewModel::
                        toggleExtra,

                        onContinua = {

                            /*
                             * TODO:
                             * collegare l'ID reale di:
                             *
                             * disponibilitaItinerarioId
                             * oppure
                             * sessioneSingolaAttivitaId
                             */
                        }
                    )
                }


                /*
                 * STEP 2
                 */

                composable(
                    AppDestination.BookingStep2.route
                ) { backStackEntry ->

                    val parentEntry =
                        remember(backStackEntry) {

                            navController
                                .getBackStackEntry(
                                    "booking_graph"
                                )
                        }

                    val bookingContext =
                        LocalContext.current

                    val bookingViewModel:
                            PrenotazioniViewModel =
                        viewModel(
                            viewModelStoreOwner =
                            parentEntry,

                            factory =
                            PrenotazioniViewModelFactory(
                                bookingContext
                            )
                        )

                    val state by
                    bookingViewModel
                        .uiState
                        .collectAsState()


                    LaunchedEffect(
                        state.pagamentoCompletato
                    ) {

                        if (
                            state.pagamentoCompletato != null
                        ) {

                            navController.navigate(
                                AppDestination
                                    .BookingSuccess
                                    .route
                            )
                        }
                    }


                    PrenotazionePasso2Screen(
                        uiState = state,

                        onMetodoPagamentoSelezionato =
                        bookingViewModel::
                        selezionaMetodoPagamento,

                        onConfermaEPaga = {

                            bookingViewModel
                                .pagaPrenotazione()
                        }
                    )
                }


                /*
                 * SUCCESSO
                 */

                composable(
                    AppDestination.BookingSuccess.route
                ) { backStackEntry ->

                    val parentEntry =
                        remember(backStackEntry) {

                            navController
                                .getBackStackEntry(
                                    "booking_graph"
                                )
                        }

                    val bookingContext =
                        LocalContext.current

                    val bookingViewModel:
                            PrenotazioniViewModel =
                        viewModel(
                            viewModelStoreOwner =
                            parentEntry,

                            factory =
                            PrenotazioniViewModelFactory(
                                bookingContext
                            )
                        )

                    val state by
                    bookingViewModel
                        .uiState
                        .collectAsState()


                    PrenotazioneSuccessoScreen(
                        uiState = state,

                        onFine = {

                            bookingViewModel
                                .resetBooking()

                            navController.navigate(
                                AppDestination
                                    .Bookings
                                    .route
                            ) {

                                popUpTo(
                                    "booking_graph"
                                ) {
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


/*
 * ================================================================
 * FAVORITES
 * ================================================================
 */

@Composable
private fun FavoritesRoute(
    onBack: () -> Unit,
    onItinerarioClick: (Itinerario) -> Unit,
    viewModel: PreferitiViewModel = viewModel()
) {

    val state by
    viewModel
        .state
        .collectAsState()


    FavoritesScreen(
        state = state,
        onBack = onBack,

        onSectionChange =
        viewModel::cambiaSezione,

        onOpenList =
        viewModel::apriLista,

        onCloseList =
        viewModel::chiudiLista,

        onCreateList =
        viewModel::creaLista,

        onChangeVisibility =
        viewModel::cambiaVisibilita,

        onDeleteList =
        viewModel::eliminaLista,

        onRemoveTrip =
        viewModel::rimuoviItinerario,

        onShareWithEmail =
        viewModel::condividiConEmail,

        onRevokeShare =
        viewModel::revocaCondivisione,

        /*
         * Il dettaglio della lista conosce solo l'id:
         * l'Itinerario completo, che serve alla schermata
         * di dettaglio, e' quello gia' caricato nella
         * lista aperta.
         */
        onTripClick = { itinerarioId ->

            state.listaAperta
                ?.itinerari
                ?.firstOrNull { itinerario ->
                    itinerario.id == itinerarioId
                }
                ?.let(onItinerarioClick)
        },

        onMessageShown =
        viewModel::messaggioMostrato
    )
}


/*
 * ================================================================
 * PROFILE
 * ================================================================
 */

@Composable
private fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfiloViewModel = viewModel()
) {

    val state by
    viewModel
        .state
        .collectAsState()


    val photoPicker =
        rememberLauncherForActivityResult(
            contract =
            ActivityResultContracts
                .PickVisualMedia()
        ) { uri ->

            if (uri != null) {

                viewModel
                    .cambiaFotoProfilo(
                        uri
                    )
            }
        }


    ProfileScreen(
        state = state,
        onBack = onBack,

        onBookingsClick = {

            onNavigateTo(
                AppDestination.Bookings
            )
        },

        onFavoritesClick = {

            onNavigateTo(
                AppDestination.Favorites
            )
        },

        onAddProfilePhoto = {

            photoPicker.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts
                        .PickVisualMedia
                        .ImageOnly
                )
            )
        },

        onPhotoMessageShown =
        viewModel::messaggioMostrato,


        /*
         * CATALOGO
         */

        onCreaItinerarioClick = {

            onNavigateToRoute(
                CatalogRoutes.CREA_ITINERARIO
            )
        },

        onCreaAttivitaClick = {

            onNavigateToRoute(
                CatalogRoutes.CREA_ATTIVITA
            )
        },

        onLeMieOfferteClick = {

            onNavigateToRoute(
                CatalogRoutes.LE_MIE_OFFERTE
            )
        },

        onGestioneOfferteAdminClick = {

            onNavigateToRoute(
                CatalogRoutes.OFFERTE_ADMIN
            )
        },

        onGestioneUtentiAdminClick = {

            onNavigateToRoute(
                CatalogRoutes
                    .GESTIONE_UTENTI_ADMIN
            )
        },


        /*
         * BOOKING / PAGAMENTI
         */

        onPaymentsClick = {

            onNavigateTo(
                AppDestination.Payments
            )
        },

        onReviewsClick = {},

        onToggleDarkMode =
        viewModel::cambiaTemaScuro,

        onChangePassword = {
            onNavigateToRoute(ProfiloRoutes.CAMBIA_PASSWORD)
        },

        onLogout = onLogout
    )
}


/*
 * ================================================================
 * BOOKINGS
 * ================================================================
 */

@Composable
private fun BookingsRoute(
    viewModel: BookingsViewModel =
        viewModel()
) {

    val state by
    viewModel
        .uiState
        .collectAsState()

    val prenotazioneSelezionata =
        state.prenotazioneSelezionata


    if (
        prenotazioneSelezionata != null
    ) {

        PrenotazioneDettaglioScreen(
            prenotazione =
            prenotazioneSelezionata,

            onBack = {

                viewModel
                    .chiudiDettaglio()
            },

            onAnnulla = {

                viewModel
                    .annullaPrenotazione()
            },

            isLoading =
            state.isLoading
        )

    } else {

        BookingsScreen(
            prenotazioni =
            state.prenotazioni,

            isLoading =
            state.isLoading,

            errore =
            state.errore,

            onRiprova = {

                viewModel
                    .caricaPrenotazioni()
            },

            onPrenotazioneClick = {

                viewModel
                    .selezionaPrenotazione(
                        it
                    )
            }
        )
    }
}


/*
 * ================================================================
 * PAYMENTS
 * ================================================================
 */

@Composable
private fun PaymentsRoute(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel =
        viewModel()
) {

    val state by
    viewModel
        .uiState
        .collectAsState()


    PaymentsScreen(
        pagamenti =
        state.pagamenti,

        isLoading =
        state.isLoading,

        errore =
        state.errore,

        onRiprova = {

            viewModel
                .caricaPagamenti()
        },

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