package com.example.travelapp.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.consumeWindowInsets
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


    /*
     * ============================================================
     * BOOKING VIEWMODEL CONDIVISO
     * ============================================================
     *
     * Viene condiviso da:
     *
     * dettaglio -> step 1 -> step 2 -> successo
     *
     * In questo modo manteniamo:
     *
     * - disponibilità/sessione scelta
     * - date
     * - posti disponibili
     * - extra
     * - partecipanti
     * - pagamento
     */
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


    /*
     * ============================================================
     * BOOKINGS VIEWMODEL CONDIVISO
     * ============================================================
     *
     * Serve anche dopo:
     *
     * - una nuova prenotazione
     * - una recensione
     *
     * per aggiornare immediatamente gli elenchi.
     */
    val bookingsViewModel: BookingsViewModel =
        viewModel()


    /*
     * ============================================================
     * PARTENZE ORGANIZZATORE
     * ============================================================
     *
     * Condiviso fra l'elenco delle partenze e quello dei prenotati:
     * la seconda schermata mostra in testata la partenza scelta
     * nella prima, che quindi non va riletta dalla rete.
     */
    val partenzeViewModel: PartenzeOrganizzatoreViewModel =
        viewModel()


    /*
     * ============================================================
     * PROFILO
     * ============================================================
     */

    val profiloState by
    profiloViewModel
        .state
        .collectAsState()


    /*
     * ============================================================
     * LOGOUT
     * ============================================================
     *
     * Usiamo GestoreSessione invece di cancellare soltanto
     * il token, così viene rimossa l'intera sessione salvata.
     */
    val eseguiLogout: () -> Unit = {

        coroutineScope.launch {

            GestoreSessione.logout(
                context
            )

            onLogout()
        }
    }


    /*
     * ============================================================
     * BACK
     * ============================================================
     */

    val onBack: () -> Unit = {

        if (
            !navController.popBackStack()
        ) {

            onExitApp()
        }
    }


    /*
     * ============================================================
     * TEMA
     * ============================================================
     */

    LaunchedEffect(
        profiloState.isDarkModeEnabled,
        profiloState.id
    ) {

        /*
         * Aspettiamo il profilo reale.
         *
         * Altrimenti il valore iniziale dello state potrebbe
         * sovrascrivere per un istante il tema di sistema.
         */
        if (
            profiloState.id != null
        ) {

            onDarkModeChanged(
                profiloState.isDarkModeEnabled
            )
        }
    }


    /*
     * ============================================================
     * RUOLO
     * ============================================================
     */

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


    /*
     * ============================================================
     * ELEMENTI CATALOGO SELEZIONATI
     * ============================================================
     */

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


    /*
     * ============================================================
     * RECENSIONE
     * ============================================================
     *
     * Si può arrivare alla recensione:
     *
     * 1. dalla lista "Viaggi conclusi"
     * 2. da una notifica
     * 3. da "Le mie recensioni", per modificarne una gia' scritta
     */

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


    /*
     * ============================================================
     * REDIRECT ADMIN / ORGANIZZATORE
     * ============================================================
     */

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


    /*
     * ============================================================
     * BOTTOM BAR
     * ============================================================
     */

    val navBackStackEntry by
    navController
        .currentBackStackEntryAsState()

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


    /*
     * Nascondiamo la bottom bar anche nelle schermate
     * specifiche di Admin/Organizzatore.
     *
     * È utile anche durante il brevissimo intervallo
     * in cui il ruolo potrebbe essere ancora in caricamento.
     */
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


    /*
     * ============================================================
     * SCAFFOLD
     * ============================================================
     */

    Scaffold(
        modifier = modifier,
        containerColor =
            BackgroundLavender,

        bottomBar = {

            if (
                mostraBottomBar
            ) {

                AppBottomBar(
                    navController =
                        navController
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


            /*
             * ============================================================
             * ORGANIZZATORE
             * ============================================================
             */

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


            /*
             * ============================================================
             * PARTENZE DI UN ITINERARIO (ORGANIZZATORE)
             * ============================================================
             */

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
                    }
                )
            }


            /*
             * ============================================================
             * PRENOTATI DI UNA PARTENZA (ORGANIZZATORE)
             * ============================================================
             */

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


            /*
             * ============================================================
             * EXPLORE
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


            /*
             * ============================================================
             * PRENOTAZIONI
             * ============================================================
             */

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


            /*
             * ============================================================
             * NOTIFICHE
             * ============================================================
             */

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


            /*
             * ============================================================
             * RECENSIONE
             * ============================================================
             */

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

                            /*
                             * Aggiorniamo immediatamente
                             * "Viaggi conclusi".
                             */
                            bookingsViewModel
                                .caricaPrenotazioni()

                            onBack()
                        }
                    )
                }
            }


            /*
             * ============================================================
             * LE MIE RECENSIONI
             * ============================================================
             */

            composable(
                EsperienzaRoutes
                    .MIE_RECENSIONI
            ) {

                MieRecensioniRoute(

                    onBack =
                        onBack,

                    onModifica = { recensione ->

                        /*
                         * Il form riparte dalla prenotazione:
                         * azzeriamo le altre due provenienze.
                         */
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


            /*
             * ============================================================
             * PAGAMENTI
             * ============================================================
             */

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


            /*
             * ============================================================
             * PREFERITI
             * ============================================================
             */

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


            /*
             * ============================================================
             * PROFILO
             * ============================================================
             */

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


            /*
             * ============================================================
             * CAMBIO PASSWORD
             * ============================================================
             */

            composable(
                ProfiloRoutes
                    .CAMBIA_PASSWORD
            ) {

                CambiaPasswordScreen(

                    onBack =
                        onBack,

                    /*
                     * Il backend invalida le sessioni.
                     */
                    onPasswordCambiata =
                        eseguiLogout
                )
            }


            /*
             * ============================================================
             * DETTAGLIO ITINERARIO
             * ============================================================
             */

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

                            /*
                             * DetailScreen restituisce
                             * l'intero DTO della disponibilità.
                             */
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

                                        /*
                                         * Necessario per caricare
                                         * gli extra dal backend.
                                         */
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


            /*
             * ============================================================
             * DETTAGLIO ATTIVITÀ
             * ============================================================
             */

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

                            /*
                             * DetailScreen restituisce
                             * l'intero DTO della sessione.
                             */
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

                                        /*
                                         * Nessun itinerario:
                                         * le attività singole non
                                         * caricano extra.
                                         */
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


            /*
             * ============================================================
             * CREA ITINERARIO
             * ============================================================
             */

            composable(
                CatalogRoutes
                    .CREA_ITINERARIO
            ) {

                CreaItinerarioScreen(
                    onBack =
                        onBack
                )
            }


            /*
             * ============================================================
             * MODIFICA ITINERARIO
             * ============================================================
             */

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


            /*
             * ============================================================
             * CREA ATTIVITÀ
             * ============================================================
             */

            composable(
                CatalogRoutes
                    .CREA_ATTIVITA
            ) {

                CreaAttivitaScreen(
                    onBack =
                        onBack
                )
            }


            /*
             * ============================================================
             * MODIFICA ATTIVITÀ
             * ============================================================
             */

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


            /*
             * ============================================================
             * OFFERTE ORGANIZZATORE
             * ============================================================
             */

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


            /*
             * ============================================================
             * OFFERTE ADMIN
             * ============================================================
             */

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


            /*
             * ============================================================
             * GESTIONE UTENTI ADMIN
             * ============================================================
             */

            composable(
                CatalogRoutes
                    .GESTIONE_UTENTI_ADMIN
            ) {

                GestioneUtentiAdminScreen(
                    onBack =
                        onBack
                )
            }


            /*
             * ============================================================
             * BOOKING GRAPH
             * ============================================================
             */

            navigation(

                startDestination =
                    AppDestination
                        .BookingStep1
                        .route,

                route =
                    "booking_graph"

            ) {


                /*
                 * ========================================================
                 * STEP 1
                 * ========================================================
                 */

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

                                /*
                                 * Non vogliamo creare nuovamente
                                 * la stessa prenotazione tornando
                                 * allo step precedente.
                                 */
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

                        /*
                         * IMPORTANTE:
                         *
                         * non deve tornare emptyList().
                         */
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

                        /*
                         * Gli ID sono già dentro BookingUiState.
                         */
                        onContinua = {

                            bookingViewModel
                                .creaPrenotazione()
                        }
                    )
                }


                /*
                 * ========================================================
                 * STEP 2
                 * ========================================================
                 */

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


                /*
                 * ========================================================
                 * SUCCESSO
                 * ========================================================
                 */

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

                            /*
                             * La prenotazione appena creata
                             * deve apparire subito nella lista.
                             */
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


        /*
         * ============================================================
         * VIAGGIATORE
         * ============================================================
         */

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


        /*
         * ============================================================
         * FOTO
         * ============================================================
         */

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


        /*
         * ============================================================
         * ORGANIZZATORE
         * ============================================================
         */

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


        /*
         * ============================================================
         * ADMIN
         * ============================================================
         */

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


        /*
         * ============================================================
         * IMPOSTAZIONI
         * ============================================================
         */

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


/*
 * ================================================================
 * BOOKINGS
 * ================================================================
 */

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


    val prenotazioneSelezionata =
        state
            .prenotazioneSelezionata


    /*
     * Tornando da notifiche o recensioni
     * aggiorniamo il contatore.
     */
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

            /*
             * IMPORTANTE:
             *
             * Manteniamo il pagamento di una
             * prenotazione rimasta IN_ATTESA.
             */
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

            /*
             * Prenotazioni attuali.
             */
            prenotazioni =
                state.prenotazioni,

            /*
             * Nuova sezione develop.
             */
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


/*
 * ================================================================
 * NOTIFICHE
 * ================================================================
 */

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

            /*
             * Aprire la notifica equivale
             * a segnarla come letta.
             */
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


/*
 * ================================================================
 * RECENSIONE
 * ================================================================
 */

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


/*
 * ================================================================
 * PAYMENTS
 * ================================================================
 */

/*
 * ================================================================
 * LE MIE RECENSIONI
 * ================================================================
 */

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


    /*
     * Ricarichiamo anche al rientro dal form:
     * una recensione appena modificata
     * deve mostrarsi aggiornata.
     */
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