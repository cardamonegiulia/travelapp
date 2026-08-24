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
import androidx.compose.runtime.mutableStateListOf
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
import com.example.travelapp.ui.catalog.AttivitaDetailScreen
import com.example.travelapp.ui.catalog.CreaAttivitaScreen
import com.example.travelapp.ui.catalog.CreaItinerarioScreen
import com.example.travelapp.ui.catalog.GestioneUtentiAdminScreen
import com.example.travelapp.ui.catalog.ItinerarioDetailScreen
import com.example.travelapp.ui.catalog.OfferteManagementScreen
import com.example.travelapp.ui.catalog.UtenteAdminItem
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
import com.example.travelapp.ui.profilo.ProfiloViewModel
import com.example.travelapp.ui.screens.BookingsScreen
import com.example.travelapp.ui.screens.ExploreScreen
import com.example.travelapp.ui.screens.FavoritesScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.screens.sampleFavoriteTrips
import com.example.travelapp.ui.theme.BackgroundLavender
import java.math.BigDecimal


object CatalogRoutes {
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
    onExitApp: () -> Unit = {}
) {

    val context = LocalContext.current

    val onBack: () -> Unit = {
        if (!navController.popBackStack()) {
            onExitApp()
        }
    }

    /*
     * La bottom bar viene nascosta durante il wizard di prenotazione
     * per evitare che l'utente abbandoni accidentalmente il flusso.
     */
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mostraBottomBar = currentRoute !in setOf(
        AppDestination.BookingStep1.route,
        AppDestination.BookingStep2.route,
        AppDestination.BookingSuccess.route
    )

    /*
     * Dati temporanei usati dalle schermate di gestione Catalog.
     * Potranno essere sostituiti dai dati reali del backend.
     */
    val mockItinerari = remember {
        mutableStateListOf(
            Itinerario(
                id = 1L,
                organizzatoreId = 1L,
                titolo = "Tour delle Cantine del Chianti",
                descrizione = "Degustazione vini tipici toscani e visita ai vigneti storici.",
                destinazionePrincipale = "Toscana",
                prezzoBase = BigDecimal("120.00"),
                durataGiorni = 3,
                maxPartecipanti = 12,
                stato = "ATTIVO"
            ),
            Itinerario(
                id = 2L,
                organizzatoreId = 1L,
                titolo = "Escursione Vulcano Etna",
                descrizione = "Trekking guidato ai crateri sommitali e sentieri naturalistici.",
                destinazionePrincipale = "Sicilia",
                prezzoBase = BigDecimal("85.00"),
                durataGiorni = 1,
                maxPartecipanti = 15,
                stato = "ATTIVO"
            )
        )
    }

    val mockAttivita = remember {
        mutableStateListOf(
            SingolaAttivita(
                id = 101L,
                organizzatoreId = 1L,
                titolo = "Degustazione Olio EVO in Frantoio",
                descrizione = "Visita e assaggio degli oli extravergine di oliva.",
                luogo = "Firenze",
                prezzo = BigDecimal("35.00"),
                durataMinuti = 120,
                maxPartecipanti = 10
            ),
            SingolaAttivita(
                id = 102L,
                organizzatoreId = 1L,
                titolo = "Corso di Pasta Fresca Fatta a Mano",
                descrizione = "Impara a preparare tagliatelle e ravioli tradizionali.",
                luogo = "Bologna",
                prezzo = BigDecimal("50.00"),
                durataMinuti = 180,
                maxPartecipanti = 8
            )
        )
    }

    val mockUtenti = remember {
        listOf(
            UtenteAdminItem(
                1L,
                "Mario Rossi",
                "mario@example.it",
                "VIAGGIATORE"
            ),
            UtenteAdminItem(
                2L,
                "Elena Bianchi",
                "elena@organizer.it",
                "ORGANIZZATORE"
            ),
            UtenteAdminItem(
                3L,
                "Luca Conti",
                "luca.c@example.it",
                "VIAGGIATORE"
            ),
            UtenteAdminItem(
                4L,
                "Alessandro Ricci",
                "a.ricci@organizer.it",
                "ORGANIZZATORE"
            )
        )
    }

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
            startDestination = AppDestination.start.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * DESTINAZIONI PRINCIPALI
             */

            composable(AppDestination.Explore.route) {

                ExploreScreen(
                    onItinerarioClick = { itinerario ->
                        itinerarioSelezionato = itinerario

                        navController.navigate(
                            CatalogRoutes.DETTAGLIO_ITINERARIO
                        )
                    },

                    onAttivitaClick = { attivita ->
                        attivitaSelezionata = attivita

                        navController.navigate(
                            CatalogRoutes.DETTAGLIO_ATTIVITA
                        )
                    }
                )
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
                FavoritesRoute(
                    onBack = onBack
                )
            }


            composable(AppDestination.Profile.route) {

                ProfileRoute(
                    onBack = onBack,

                    onNavigateTo = { destination ->
                        navController.navigate(
                            destination.route
                        )
                    },

                    onNavigateToRoute = { route ->
                        navController.navigate(route)
                    }
                )
            }


            /*
             * DETTAGLIO CATALOGO
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
                             * TODO booking:
                             * invece di andare direttamente a Bookings,
                             * qui collegheremo l'ID reale al booking_graph.
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
                             * TODO booking:
                             * qui collegheremo sessioneId al booking_graph.
                             */
                            navController.navigate(
                                AppDestination.Bookings.route
                            )
                        }
                    )
                }
            }


            /*
             * CREAZIONE E MODIFICA CATALOGO
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
             * GESTIONE OFFERTE
             */

            composable(
                CatalogRoutes.LE_MIE_OFFERTE
            ) {

                OfferteManagementScreen(
                    isAdmin = false,
                    itinerari = mockItinerari,
                    attivita = mockAttivita,
                    onBack = onBack,

                    onModificaItinerario = { item ->

                        itinerarioInModifica = item

                        navController.navigate(
                            CatalogRoutes.MODIFICA_ITINERARIO
                        )
                    },

                    onEliminaItinerario = { id ->

                        mockItinerari.removeAll {
                            it.id == id
                        }

                        Toast.makeText(
                            context,
                            "Itinerario eliminato!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },

                    onModificaAttivita = { item ->

                        attivitaInModifica = item

                        navController.navigate(
                            CatalogRoutes.MODIFICA_ATTIVITA
                        )
                    },

                    onEliminaAttivita = { id ->

                        mockAttivita.removeAll {
                            it.id == id
                        }

                        Toast.makeText(
                            context,
                            "Attività eliminata!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }


            /*
             * GESTIONE OFFERTE ADMIN
             */

            composable(
                CatalogRoutes.OFFERTE_ADMIN
            ) {

                OfferteManagementScreen(
                    isAdmin = true,
                    itinerari = mockItinerari,
                    attivita = mockAttivita,
                    onBack = onBack,

                    onModificaItinerario = { item ->

                        itinerarioInModifica = item

                        navController.navigate(
                            CatalogRoutes.MODIFICA_ITINERARIO
                        )
                    },

                    onEliminaItinerario = { id ->

                        mockItinerari.removeAll {
                            it.id == id
                        }

                        Toast.makeText(
                            context,
                            "[ADMIN] Itinerario eliminato!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },

                    onModificaAttivita = { item ->

                        attivitaInModifica = item

                        navController.navigate(
                            CatalogRoutes.MODIFICA_ATTIVITA
                        )
                    },

                    onEliminaAttivita = { id ->

                        mockAttivita.removeAll {
                            it.id == id
                        }

                        Toast.makeText(
                            context,
                            "[ADMIN] Attività eliminata!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }


            composable(
                CatalogRoutes.GESTIONE_UTENTI_ADMIN
            ) {

                GestioneUtentiAdminScreen(
                    utenti = mockUtenti,
                    onBack = onBack
                )
            }


            /*
             * BOOKING WIZARD
             *
             * Tutti gli step condividono la stessa istanza
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
                            navController.getBackStackEntry(
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
                    bookingViewModel.uiState
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
                             * Verrà collegato agli ID reali
                             * disponibilitaItinerarioId /
                             * sessioneSingolaAttivitaId.
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
                    bookingViewModel.uiState
                        .collectAsState()


                    LaunchedEffect(
                        state.pagamentoCompletato
                    ) {

                        if (
                            state.pagamentoCompletato !=
                            null
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
                    bookingViewModel.uiState
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
 * FAVORITES
 */

@Composable
private fun FavoritesRoute(
    onBack: () -> Unit
) {

    var trips by remember {
        mutableStateOf(
            sampleFavoriteTrips
        )
    }

    FavoritesScreen(
        trips = trips,
        onBack = onBack,

        onToggleFavorite = { id ->

            trips = trips.map { trip ->

                if (trip.id == id) {
                    trip.copy(
                        isFavorite =
                        !trip.isFavorite
                    )
                } else {
                    trip
                }
            }
        },

        onLoadMore = {},
        onTripClick = {}
    )
}


/*
 * PROFILE
 */

@Composable
private fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    viewModel: ProfiloViewModel = viewModel()
) {

    val state by
    viewModel.state.collectAsState()


    val photoPicker =
        rememberLauncherForActivityResult(
            contract =
            ActivityResultContracts
                .PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                viewModel
                    .cambiaFotoProfilo(uri)
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
         * CATALOGO / ORGANIZZATORE / ADMIN
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

        onChangePassword = {},

        onLogout = {}
    )
}


/*
 * BOOKINGS
 */

@Composable
private fun BookingsRoute(
    viewModel: BookingsViewModel =
        viewModel()
) {

    val state by
    viewModel.uiState.collectAsState()

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
                    .selezionaPrenotazione(it)
            }
        )
    }
}


/*
 * PAYMENTS
 */

@Composable
private fun PaymentsRoute(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel =
        viewModel()
) {

    val state by
    viewModel.uiState.collectAsState()

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