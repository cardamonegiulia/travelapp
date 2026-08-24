package com.example.travelapp.ui.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.catalog.*
import com.example.travelapp.ui.components.AppBottomBar
import com.example.travelapp.ui.profilo.ProfiloViewModel
import com.example.travelapp.ui.screens.BookingsScreen
import com.example.travelapp.ui.screens.ExploreScreen
import com.example.travelapp.ui.screens.FavoritesScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.screens.sampleFavoriteTrips
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
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val profiloState by profiloViewModel.state.collectAsState()
    val onBack: () -> Unit = { if (!navController.popBackStack()) onExitApp() }

    val ruoloStr = profiloState.ruolo?.toString()?.uppercase() ?: ""
    val isAdmin = ruoloStr.contains("ADMIN")
    val isOrganizzatore = ruoloStr.contains("ORGANIZZATORE")

    // Reindirizzamento reattivo all'arrivo del ruolo dal profilo
    LaunchedEffect(profiloState.ruolo) {
        when {
            isAdmin -> {
                navController.navigate(CatalogRoutes.ADMIN_HOME) {
                    popUpTo(AppDestination.Explore.route) { inclusive = true }
                }
            }
            isOrganizzatore -> {
                navController.navigate(CatalogRoutes.ORGANIZZATORE_HOME) {
                    popUpTo(AppDestination.Explore.route) { inclusive = true }
                }
            }
        }
    }

    var itinerarioSelezionato by remember { mutableStateOf<Itinerario?>(null) }
    var attivitaSelezionata by remember { mutableStateOf<SingolaAttivita?>(null) }
    var itinerarioInModifica by remember { mutableStateOf<Itinerario?>(null) }
    var attivitaInModifica by remember { mutableStateOf<SingolaAttivita?>(null) }

    val mostraBottomBar = !isAdmin && !isOrganizzatore

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
            startDestination = AppDestination.Explore.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- HOME DEDICATA ADMIN ---
            composable(CatalogRoutes.ADMIN_HOME) {
                AdminDashboardScreen(
                    onVaiOfferte = { navController.navigate(CatalogRoutes.OFFERTE_ADMIN) },
                    onVaiUtenti = { navController.navigate(CatalogRoutes.GESTIONE_UTENTI_ADMIN) },
                    onLogout = { onExitApp() }
                )
            }

            // --- HOME DEDICATA ORGANIZZATORE ---
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
                    onLogout = { onExitApp() }
                )
            }

            // --- VIAGGIATORE EXPERIENCE ---
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
                BookingsScreen()
            }

            composable(AppDestination.Favorites.route) {
                FavoritesRoute(onBack = onBack)
            }

            composable(AppDestination.Profile.route) {
                ProfileRoute(
                    onBack = onBack,
                    onNavigateTo = { destination -> navController.navigate(destination.route) },
                    onNavigateToRoute = { route -> navController.navigate(route) },
                    viewModel = profiloViewModel
                )
            }

            // --- Dettaglio con Date / Sessioni reali ---
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

            // --- Creazione e Modifica Catalogo ---
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

            // --- Gestione Offerte e Admin ---
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
                GestioneUtentiAdminScreen(
                    onBack = onBack
                )
            }
        }
    }
}

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
        onTripClick = {}
    )
}

@Composable
private fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    onNavigateToRoute: (String) -> Unit,
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
        onFavoritesClick = { onNavigateTo(AppDestination.Favorites) },
        onAddProfilePhoto = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onPhotoMessageShown = viewModel::messaggioMostrato,
        onCreaItinerarioClick = { onNavigateToRoute(CatalogRoutes.CREA_ITINERARIO) },
        onCreaAttivitaClick = { onNavigateToRoute(CatalogRoutes.CREA_ATTIVITA) },
        onLeMieOfferteClick = { onNavigateToRoute(CatalogRoutes.LE_MIE_OFFERTE) },
        onGestioneOfferteAdminClick = { onNavigateToRoute(CatalogRoutes.OFFERTE_ADMIN) },
        onGestioneUtentiAdminClick = { onNavigateToRoute(CatalogRoutes.GESTIONE_UTENTI_ADMIN) },
        onPaymentsClick = {},
        onReviewsClick = {},
        onToggleDarkMode = viewModel::cambiaTemaScuro,
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