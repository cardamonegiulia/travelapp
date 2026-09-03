package com.example.travelapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.travelapp.data.remote.GestoreSessione
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.Notifica
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.domain.model.Recensione
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.catalog.AdminDashboardScreen
import com.example.travelapp.ui.catalog.AttivitaDetailScreen
import com.example.travelapp.ui.catalog.CreaAttivitaScreen
import com.example.travelapp.ui.catalog.CreaItinerarioScreen
import com.example.travelapp.ui.catalog.GestioneUtentiAdminScreen
import com.example.travelapp.ui.catalog.ItinerarioDetailScreen
import com.example.travelapp.ui.catalog.OfferteManagementScreen
import com.example.travelapp.ui.catalog.OrganizzatoreHomeScreen
import com.example.travelapp.ui.catalog.PartenzeItinerarioScreen
import com.example.travelapp.ui.catalog.PartenzeOrganizzatoreViewModel
import com.example.travelapp.ui.catalog.PrenotatiPartenzaScreen
import com.example.travelapp.ui.components.AppBottomBar
import com.example.travelapp.ui.notifiche.NotificheScreen
import com.example.travelapp.ui.notifiche.NotificheViewModel
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
import com.example.travelapp.ui.recensioni.MieRecensioniScreen
import com.example.travelapp.ui.recensioni.MieRecensioniViewModel
import com.example.travelapp.ui.recensioni.RecensioneScreen
import com.example.travelapp.ui.recensioni.RecensioneViewModel
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

    const val PARTENZE_ITINERARIO =
        "catalog/partenze_itinerario"

    const val PRENOTATI_PARTENZA =
        "catalog/prenotati_partenza"
}


object EsperienzaRoutes {

    const val NOTIFICHE =
        "esperienza/notifiche"

    const val RECENSIONE =
        "esperienza/recensione"

    const val MIE_RECENSIONI =
        "esperienza/mie_recensioni"
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

    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()

    val bookingViewModel: PrenotazioniViewModel =
        viewModel(
            factory =
                PrenotazioniViewModelFactory(
                    context
                )
        )

    val bookingState by
    bookingViewModel
        .uiState
        .collectAsState()

    val bookingsViewModel: BookingsViewModel =
        viewModel()

    val partenzeViewModel: PartenzeOrganizzatoreViewModel =
        viewModel()


    val profiloState by
    profiloViewModel
        .state
        .collectAsState()


    val eseguiLogout: () -> Unit = {

        coroutineScope.launch {

            GestoreSessione.logout(
                context
            )

            onLogout()
        }
    }


    var mostraConfermaUscita by remember {
        mutableStateOf(false)
    }

    val onBack: () -> Unit = {

        if (
            !navController.popBackStack()
        ) {

            mostraConfermaUscita = true
        }
    }

    LaunchedEffect(
        profiloState.isDarkModeEnabled,
        profiloState.id
    ) {

        if (
            profiloState.id != null
        ) {

            onDarkModeChanged(
                profiloState.isDarkModeEnabled
            )
        }
    }


    val ruoloStr =
        profiloState.ruolo
            ?.toString()
            ?.uppercase()
            ?: ""

    val isAdmin =
        ruoloStr.contains(
            "ADMIN"
        )

    val isOrganizzatore =
        ruoloStr.contains(
            "ORGANIZZATORE"
        )


    var itinerarioSelezionato by remember {
        mutableStateOf<Itinerario?>(
            null
        )
    }

    var attivitaSelezionata by remember {
        mutableStateOf<SingolaAttivita?>(
            null
        )
    }

    var itinerarioInModifica by remember {
        mutableStateOf<Itinerario?>(
            null
        )
    }

    var attivitaInModifica by remember {
        mutableStateOf<SingolaAttivita?>(
            null
        )
    }

    var viaggioDaRecensire by remember {
        mutableStateOf<Prenotazione?>(
            null
        )
    }

    var recensioneDaNotifica by remember {
        mutableStateOf<Notifica?>(
            null
        )
    }

    var recensioneDaModificare by remember {
        mutableStateOf<Recensione?>(
            null
        )
    }


    LaunchedEffect(
        profiloState.ruolo
    ) {

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

                    launchSingleTop =
                        true
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

                    launchSingleTop =
                        true
                }
            }
        }
    }


    val navBackStackEntry by
    navController
        .currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry
            ?.destination
            ?.route
    val siamoAllaRadice =
        navController.previousBackStackEntry == null

    BackHandler(
        enabled =
            siamoAllaRadice &&
                    !mostraConfermaUscita
    ) {

        mostraConfermaUscita = true
    }


    val isBookingWizard =
        currentRoute in setOf(
            AppDestination.BookingStep1.route,
            AppDestination.BookingStep2.route,
            AppDestination.BookingSuccess.route
        )


    val isCatalogAdminOrOrg =
        currentRoute in setOf(
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


    val mostraBottomBar =
        !isAdmin &&
                !isOrganizzatore &&
                !isBookingWizard &&
                !isCatalogAdminOrOrg

    Scaffold(
        modifier = modifier,
        containerColor =
            BackgroundLavender,

        bottomBar = {

            if (
                mostraBottomBar
            ) {

                AppBottomBar(
                    navController = navController,
                    onDestinationSelected = { destination ->
                        if (destination == AppDestination.Bookings) {
                            bookingsViewModel.chiudiDettaglio()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->


        NavHost(
            navController =
                navController,

            startDestination =
                AppDestination
                    .Explore
                    .route,

            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .consumeWindowInsets(
                    innerPadding
                )
        ) {

            composable(
                CatalogRoutes.ADMIN_HOME
            ) {

                AdminDashboardScreen(

                    onVaiOfferte = {

                        navController.navigate(
                            CatalogRoutes
                                .OFFERTE_ADMIN
                        )
                    },

                    onVaiUtenti = {

                        navController.navigate(
                            CatalogRoutes
                                .GESTIONE_UTENTI_ADMIN
                        )
                    },

                    onVaiProfilo = {

                        navController.navigate(
                            AppDestination
                                .Profile
                                .route
                        ) {

                            launchSingleTop =
                                true
                        }
                    },

                    onLogout =
                        eseguiLogout
                )
            }

            composable(
                CatalogRoutes
                    .ORGANIZZATORE_HOME
            ) {

                OrganizzatoreHomeScreen(

                    onCreaItinerario = {

                        navController.navigate(
                            CatalogRoutes
                                .CREA_ITINERARIO
                        )
                    },

                    onCreaAttivita = {

                        navController.navigate(
                            CatalogRoutes
                                .CREA_ATTIVITA
                        )
                    },

                    onModificaItinerario = { item ->

                        itinerarioInModifica =
                            item

                        navController.navigate(
                            CatalogRoutes
                                .MODIFICA_ITINERARIO
                        )
                    },

                    onModificaAttivita = { item ->

                        attivitaInModifica =
                            item

                        navController.navigate(
                            CatalogRoutes
                                .MODIFICA_ATTIVITA
                        )
                    },

                    onVediPrenotazioni = { item ->

                        partenzeViewModel.caricaPartenze(
                            itinerarioId = item.id,
                            titoloItinerario = item.titolo
                        )

                        navController.navigate(
                            CatalogRoutes
                                .PARTENZE_ITINERARIO
                        )
                    },

                    onVaiProfilo = {

                        navController.navigate(
                            AppDestination
                                .Profile
                                .route
                        ) {

                            launchSingleTop =
                                true
                        }
                    },

                    onLogout =
                        eseguiLogout
                )
            }

            composable(
                CatalogRoutes
                    .PARTENZE_ITINERARIO
            ) {

                val partenzeState by
                partenzeViewModel
                    .partenze
                    .collectAsState()

                PartenzeItinerarioScreen(

                    state =
                        partenzeState,

                    onBack =
                        onBack,

                    onPartenzaClick = { partenza ->

                        partenzeViewModel
                            .caricaPrenotati(
                                partenza
                            )

                        navController.navigate(
                            CatalogRoutes
                                .PRENOTATI_PARTENZA
                        )
                    },

                    onRiprova = {

                        partenzeViewModel
                            .ricaricaPartenze()
                    },

                    onEliminaPartenza = { partenza ->

                        partenzeViewModel
                            .chiediConfermaEliminazione(
                                partenza
                            )
                    },

                    onConfermaEliminazione = {

                        partenzeViewModel
                            .confermaEliminazione()
                    },

                    onAnnullaEliminazione = {

                        partenzeViewModel
                            .annullaEliminazione()
                    },

                    onMessaggioMostrato = {

                        partenzeViewModel
                            .messaggioMostrato()
                    }
                )
            }


            composable(
                CatalogRoutes
                    .PRENOTATI_PARTENZA
            ) {

                val prenotatiState by
                partenzeViewModel
                    .prenotati
                    .collectAsState()

                PrenotatiPartenzaScreen(

                    state =
                        prenotatiState,

                    onBack =
                        onBack,

                    onRiprova = {

                        partenzeViewModel
                            .ricaricaPrenotati()
                    }
                )
            }

            composable(
                AppDestination.Explore.route
            ) {

                ExploreScreen(

                    onItinerarioClick = { itinerario ->

                        itinerarioSelezionato =
                            itinerario

                        navController.navigate(
                            CatalogRoutes
                                .DETTAGLIO_ITINERARIO
                        )
                    },

                    onAttivitaClick = { attivita ->

                        attivitaSelezionata =
                            attivita

                        navController.navigate(
                            CatalogRoutes
                                .DETTAGLIO_ATTIVITA
                        )
                    }
                )
            }

            composable(
                AppDestination.Bookings.route
            ) {

                BookingsRoute(

                    viewModel =
                        bookingsViewModel,

                    onRecensisci = { prenotazione ->

                        viaggioDaRecensire =
                            prenotazione

                        recensioneDaNotifica =
                            null

                        navController.navigate(
                            EsperienzaRoutes
                                .RECENSIONE
                        )
                    },

                    onNotifiche = {

                        navController.navigate(
                            EsperienzaRoutes
                                .NOTIFICHE
                        )
                    }
                )
            }

            composable(
                EsperienzaRoutes.NOTIFICHE
            ) {

                NotificheRoute(

                    onBack =
                        onBack,

                    onApriRecensione = { notifica ->

                        recensioneDaNotifica =
                            notifica

                        viaggioDaRecensire =
                            null

                        navController.navigate(
                            EsperienzaRoutes
                                .RECENSIONE
                        )
                    }
                )
            }

            composable(
                EsperienzaRoutes.RECENSIONE
            ) {

                val prenotazioneId =
                    viaggioDaRecensire?.id
                        ?: recensioneDaNotifica
                            ?.prenotazioneId
                        ?: recensioneDaModificare
                            ?.prenotazioneId

                val titoloViaggio =
                    viaggioDaRecensire?.titolo
                        ?: recensioneDaNotifica
                            ?.titoloViaggio
                        ?: recensioneDaModificare
                            ?.titoloItinerario
                        ?: ""


                if (
                    prenotazioneId != null
                ) {

                    RecensioneRoute(

                        prenotazioneId =
                            prenotazioneId,

                        titoloViaggio =
                            titoloViaggio,

                        onBack =
                            onBack,

                        onSalvata = {

                            bookingsViewModel
                                .caricaPrenotazioni()

                            onBack()
                        }
                    )
                }
            }


            composable(
                EsperienzaRoutes
                    .MIE_RECENSIONI
            ) {

                MieRecensioniRoute(

                    onBack =
                        onBack,

                    onModifica = { recensione ->

                        recensioneDaModificare =
                            recensione

                        viaggioDaRecensire =
                            null

                        recensioneDaNotifica =
                            null

                        navController.navigate(
                            EsperienzaRoutes
                                .RECENSIONE
                        )
                    }
                )
            }

            composable(
                AppDestination.Payments.route
            ) {

                PaymentsRoute(

                    onBack = {

                        navController
                            .popBackStack()
                    }
                )
            }

            composable(
                AppDestination.Favorites.route
            ) {

                FavoritesRoute(

                    onBack =
                        onBack,

                    onItinerarioClick = { itinerario ->

                        itinerarioSelezionato =
                            itinerario

                        navController.navigate(
                            CatalogRoutes
                                .DETTAGLIO_ITINERARIO
                        )
                    }
                )
            }

            composable(
                AppDestination.Profile.route
            ) {

                ProfileRoute(

                    viewModel =
                        profiloViewModel,

                    onBack =
                        onBack,

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

                    onLogout =
                        eseguiLogout
                )
            }

            composable(
                ProfiloRoutes
                    .CAMBIA_PASSWORD
            ) {

                CambiaPasswordScreen(

                    onBack =
                        onBack,

                    onPasswordCambiata =
                        eseguiLogout
                )
            }

            composable(
                CatalogRoutes
                    .DETTAGLIO_ITINERARIO
            ) {

                itinerarioSelezionato
                    ?.let { item ->

                        ItinerarioDetailScreen(

                            itinerario =
                                item,

                            onBack =
                                onBack,

                            onPrenota = { disponibilita ->

                                bookingViewModel
                                    .inizializzaBooking(

                                        titolo =
                                            item.titolo,

                                        luogo =
                                            item
                                                .destinazionePrincipale
                                                ?: "",

                                        prezzoBaseUnitario =
                                            item
                                                .prezzoBase
                                                ?.toDouble()
                                                ?: 0.0,

                                        itinerarioId =
                                            item.id,

                                        disponibilitaItinerarioId =
                                            disponibilita.id,

                                        sessioneSingolaAttivitaId =
                                            null,

                                        dataInizio =
                                            disponibilita
                                                .dataInizio,

                                        dataFine =
                                            disponibilita
                                                .dataFine,

                                        postiDisponibili =
                                            disponibilita
                                                .postiDisponibili
                                    )


                                navController.navigate(
                                    AppDestination
                                        .BookingStep1
                                        .route
                                ) {

                                    launchSingleTop =
                                        true
                                }
                            }
                        )
                    }
            }

            composable(
                CatalogRoutes
                    .DETTAGLIO_ATTIVITA
            ) {

                attivitaSelezionata
                    ?.let { item ->

                        AttivitaDetailScreen(

                            attivita =
                                item,

                            onBack =
                                onBack,

                            onPrenota = { sessione ->

                                bookingViewModel
                                    .inizializzaBooking(

                                        titolo =
                                            item.titolo,

                                        luogo =
                                            item.luogo
                                                ?: "",

                                        prezzoBaseUnitario =
                                            item.prezzo
                                                ?.toDouble()
                                                ?: 0.0,

                                        itinerarioId =
                                            null,

                                        disponibilitaItinerarioId =
                                            null,

                                        sessioneSingolaAttivitaId =
                                            sessione.id,

                                        dataInizio =
                                            sessione
                                                .dataInizio,

                                        dataFine =
                                            null,

                                        postiDisponibili =
                                            sessione
                                                .postiDisponibili
                                    )


                                navController.navigate(
                                    AppDestination
                                        .BookingStep1
                                        .route
                                ) {

                                    launchSingleTop =
                                        true
                                }
                            }
                        )
                    }
            }

            composable(
                CatalogRoutes
                    .CREA_ITINERARIO
            ) {

                CreaItinerarioScreen(
                    onBack =
                        onBack
                )
            }

            composable(
                CatalogRoutes
                    .MODIFICA_ITINERARIO
            ) {

                CreaItinerarioScreen(

                    itinerarioDaModificare =
                        itinerarioInModifica,

                    onBack =
                        onBack
                )
            }

            composable(
                CatalogRoutes
                    .CREA_ATTIVITA
            ) {

                CreaAttivitaScreen(
                    onBack =
                        onBack
                )
            }

            composable(
                CatalogRoutes
                    .MODIFICA_ATTIVITA
            ) {

                CreaAttivitaScreen(

                    attivitaDaModificare =
                        attivitaInModifica,

                    onBack =
                        onBack
                )
            }

            composable(
                CatalogRoutes
                    .LE_MIE_OFFERTE
            ) {

                OfferteManagementScreen(

                    isAdmin =
                        false,

                    onBack =
                        onBack,

                    onModificaItinerario = { item ->

                        itinerarioInModifica =
                            item

                        navController.navigate(
                            CatalogRoutes
                                .MODIFICA_ITINERARIO
                        )
                    },

                    onModificaAttivita = { item ->

                        attivitaInModifica =
                            item

                        navController.navigate(
                            CatalogRoutes
                                .MODIFICA_ATTIVITA
                        )
                    }
                )
            }

            composable(
                CatalogRoutes.OFFERTE_ADMIN
            ) {

                OfferteManagementScreen(

                    isAdmin =
                        true,

                    onBack =
                        onBack
                )
            }

            composable(
                CatalogRoutes
                    .GESTIONE_UTENTI_ADMIN
            ) {

                GestioneUtentiAdminScreen(
                    onBack =
                        onBack
                )
            }


            navigation(

                startDestination =
                    AppDestination
                        .BookingStep1
                        .route,

                route =
                    "booking_graph"

            ) {

                composable(
                    AppDestination
                        .BookingStep1
                        .route
                ) {


                    LaunchedEffect(
                        bookingState
                            .prenotazioneCreata
                            ?.id
                    ) {

                        if (
                            bookingState
                                .prenotazioneCreata
                            != null
                        ) {

                            navController.navigate(
                                AppDestination
                                    .BookingStep2
                                    .route
                            ) {

                                popUpTo(
                                    AppDestination
                                        .BookingStep1
                                        .route
                                ) {
                                    inclusive =
                                        true
                                }

                                launchSingleTop =
                                    true
                            }
                        }
                    }


                    PrenotazionePasso1Screen(

                        uiState =
                            bookingState,

                        extraDisponibili =
                            bookingState
                                .extraDisponibili,

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

                            bookingViewModel
                                .creaPrenotazione()
                        }
                    )
                }


                composable(
                    AppDestination
                        .BookingStep2
                        .route
                ) {


                    LaunchedEffect(
                        bookingState
                            .pagamentoCompletato
                    ) {

                        if (
                            bookingState
                                .pagamentoCompletato
                            != null
                        ) {

                            navController.navigate(
                                AppDestination
                                    .BookingSuccess
                                    .route
                            ) {

                                popUpTo(
                                    AppDestination
                                        .BookingStep2
                                        .route
                                ) {
                                    inclusive =
                                        true
                                }

                                launchSingleTop =
                                    true
                            }
                        }
                    }


                    PrenotazionePasso2Screen(

                        uiState =
                            bookingState,

                        onMetodoPagamentoSelezionato =
                            bookingViewModel::
                            selezionaMetodoPagamento,

                        onConfermaEPaga = {

                            bookingViewModel
                                .pagaPrenotazione()
                        }
                    )
                }

                composable(
                    AppDestination
                        .BookingSuccess
                        .route
                ) {


                    PrenotazioneSuccessoScreen(

                        uiState =
                            bookingState,

                        onFine = {

                            bookingViewModel
                                .resetBooking()

                            bookingsViewModel
                                .caricaPrenotazioni()


                            navController.navigate(
                                AppDestination
                                    .Bookings
                                    .route
                            ) {

                                popUpTo(
                                    "booking_graph"
                                ) {
                                    inclusive =
                                        true
                                }

                                launchSingleTop =
                                    true
                            }
                        }
                    )
                }
            }
        }
    }
    if (mostraConfermaUscita) {

        AlertDialog(

            onDismissRequest = {
                mostraConfermaUscita = false
            },

            title = {
                Text(
                    text = "Uscire dall'app?"
                )
            },

            text = {
                Text(
                    text = "Vuoi davvero chiudere TravelApp?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        mostraConfermaUscita = false

                        onExitApp()
                    }
                ) {

                    Text(
                        text = "Esci"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        mostraConfermaUscita = false
                    }
                ) {

                    Text(
                        text = "Annulla"
                    )
                }
            }
        )
    }

}

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

        state =
            state,

        onBack =
            onBack,

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

        onTripClick = { itinerarioId ->

            state
                .listaAperta
                ?.itinerari
                ?.firstOrNull {
                    it.id ==
                            itinerarioId
                }
                ?.let(
                    onItinerarioClick
                )
        },

        onMessageShown =
            viewModel::
            messaggioMostrato
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

            if (
                uri != null
            ) {

                viewModel
                    .cambiaFotoProfilo(
                        uri
                    )
            }
        }


    ProfileScreen(

        state =
            state,

        onBack =
            onBack,

        onBookingsClick = {

            onNavigateTo(
                AppDestination.Bookings
            )
        },

        onPaymentsClick = {

            onNavigateTo(
                AppDestination.Payments
            )
        },

        onFavoritesClick = {

            onNavigateTo(
                AppDestination.Favorites
            )
        },

        onReviewsClick = {

            onNavigateToRoute(
                EsperienzaRoutes
                    .MIE_RECENSIONI
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
            viewModel::
            messaggioMostrato,

        onCreaItinerarioClick = {

            onNavigateToRoute(
                CatalogRoutes
                    .CREA_ITINERARIO
            )
        },

        onCreaAttivitaClick = {

            onNavigateToRoute(
                CatalogRoutes
                    .CREA_ATTIVITA
            )
        },

        onLeMieOfferteClick = {

            onNavigateToRoute(
                CatalogRoutes
                    .LE_MIE_OFFERTE
            )
        },

        onGestioneOfferteAdminClick = {

            onNavigateToRoute(
                CatalogRoutes
                    .OFFERTE_ADMIN
            )
        },

        onGestioneUtentiAdminClick = {

            onNavigateToRoute(
                CatalogRoutes
                    .GESTIONE_UTENTI_ADMIN
            )
        },

        onToggleDarkMode =
            viewModel::
            cambiaTemaScuro,

        onChangePassword = {

            onNavigateToRoute(
                ProfiloRoutes
                    .CAMBIA_PASSWORD
            )
        },

        onLogout =
            onLogout
    )
}

@Composable
private fun BookingsRoute(
    viewModel: BookingsViewModel = viewModel(),
    onRecensisci: (Prenotazione) -> Unit = {},
    onNotifiche: () -> Unit = {}
) {
    val state by
    viewModel
        .uiState
        .collectAsState()

    val prenotazioneSelezionata = state.prenotazioneSelezionata

    BackHandler(
        enabled = prenotazioneSelezionata != null
    ) {
        viewModel.chiudiDettaglio()
    }


    LaunchedEffect(Unit) {

        viewModel
            .aggiornaNotifiche()
    }

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
            onCompletaPagamento = {

                viewModel
                    .completaPagamento()
            },

            isLoading =
                state.isLoading,

            errore =
                state.errore
        )

    } else {

        BookingsScreen(
            prenotazioni =
                state.prenotazioni,

            viaggiConclusi =
                state.viaggiConclusi,

            schedaSelezionata =
                state.schedaSelezionata,

            notificheNonLette =
                state.notificheNonLette,

            isLoading =
                state.isLoading,

            errore =
                state.errore,

            onSchedaSelezionata =
                viewModel::
                selezionaScheda,

            onRiprova = {

                viewModel
                    .caricaPrenotazioni()
            },

            onPrenotazioneClick = {

                viewModel
                    .selezionaPrenotazione(
                        it
                    )
            },

            onRecensisci =
                onRecensisci,

            onNotificheClick =
                onNotifiche
        )
    }
}

@Composable
private fun NotificheRoute(
    onBack: () -> Unit,
    onApriRecensione: (Notifica) -> Unit,
    viewModel: NotificheViewModel = viewModel()
) {

    val state by
    viewModel
        .uiState
        .collectAsState()


    NotificheScreen(

        uiState =
            state,

        onBack =
            onBack,

        onApriRecensione = { notifica ->

            viewModel
                .segnaLetta(
                    notifica
                )

            onApriRecensione(
                notifica
            )
        },

        onRiprova =
            viewModel::carica
    )
}

@Composable
private fun RecensioneRoute(
    prenotazioneId: Long,
    titoloViaggio: String,
    onBack: () -> Unit,
    onSalvata: () -> Unit,
    viewModel: RecensioneViewModel = viewModel()
) {

    val state by
    viewModel
        .uiState
        .collectAsState()


    LaunchedEffect(
        prenotazioneId
    ) {

        viewModel.apri(
            prenotazioneId,
            titoloViaggio
        )
    }


    LaunchedEffect(
        state.salvata
    ) {

        if (
            state.salvata
        ) {

            viewModel
                .reset()

            onSalvata()
        }
    }


    RecensioneScreen(

        uiState =
            state,

        onBack =
            onBack,

        onVotazioneCambiata =
            viewModel::
            impostaVotazione,

        onCommentoCambiato =
            viewModel::
            impostaCommento,

        onSalva =
            viewModel::salva
    )
}

@Composable
private fun MieRecensioniRoute(
    onBack: () -> Unit,
    onModifica: (Recensione) -> Unit,
    viewModel: MieRecensioniViewModel = viewModel()
) {

    val state by
    viewModel
        .uiState
        .collectAsState()


    LaunchedEffect(Unit) {

        viewModel
            .caricaRecensioni()
    }


    MieRecensioniScreen(

        recensioni =
            state.recensioni,

        isLoading =
            state.isLoading,

        errore =
            state.errore,

        onRiprova = {

            viewModel
                .caricaRecensioni()
        },

        onBack =
            onBack,

        onRecensioneClick =
            onModifica
    )
}


@Composable
private fun PaymentsRoute(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel = viewModel()
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

        onBack =
            onBack
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