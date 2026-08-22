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
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.catalog.CreaAttivitaScreen
import com.example.travelapp.ui.catalog.CreaItinerarioScreen
import com.example.travelapp.ui.catalog.GestioneUtentiAdminScreen
import com.example.travelapp.ui.catalog.OfferteManagementScreen
import com.example.travelapp.ui.catalog.UtenteAdminItem
import com.example.travelapp.ui.components.AppBottomBar
import com.example.travelapp.ui.profilo.ProfiloViewModel
import com.example.travelapp.ui.screens.BookingsScreen
import com.example.travelapp.ui.screens.ExploreScreen
import com.example.travelapp.ui.screens.FavoritesScreen
import com.example.travelapp.ui.screens.ProfileScreen
import com.example.travelapp.ui.screens.sampleFavoriteTrips
import com.example.travelapp.ui.theme.BackgroundLavender
import java.math.BigDecimal

// Rotte interne per le schermate di catalogo e gestione
object CatalogRoutes {
    const val CREA_ITINERARIO = "catalog/crea_itinerario"
    const val CREA_ATTIVITA = "catalog/crea_attivita"
    const val MODIFICA_ITINERARIO = "catalog/modifica_itinerario"
    const val MODIFICA_ATTIVITA = "catalog/modifica_attivita"
    const val LE_MIE_OFFERTE = "catalog/le_mie_offerte"
    const val OFFERTE_ADMIN = "catalog/offerte_admin"
    const val GESTIONE_UTENTI_ADMIN = "catalog/gestione_utenti_admin"
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val onBack: () -> Unit = { if (!navController.popBackStack()) onExitApp() }

    // Dati mock condivisi per testare creazioni, modifiche ed eliminazioni
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
            UtenteAdminItem(1L, "Mario Rossi", "mario@example.it", "VIAGGIATORE"),
            UtenteAdminItem(2L, "Elena Bianchi", "elena@organizer.it", "ORGANIZZATORE"),
            UtenteAdminItem(3L, "Luca Conti", "luca.c@example.it", "VIAGGIATORE"),
            UtenteAdminItem(4L, "Alessandro Ricci", "a.ricci@organizer.it", "ORGANIZZATORE")
        )
    }

    var itinerarioInModifica by remember { mutableStateOf<Itinerario?>(null) }
    var attivitaInModifica by remember { mutableStateOf<SingolaAttivita?>(null) }

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
                    onNavigateTo = { destination -> navController.navigate(destination.route) },
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }

            // --- Destinazioni Catalogo e Gestione ---

            composable(CatalogRoutes.CREA_ITINERARIO) {
                CreaItinerarioScreen(
                    onBack = onBack,
                    onSalva = { dto, _ ->
                        mockItinerari.add(
                            Itinerario(
                                id = (mockItinerari.maxOfOrNull { it.id } ?: 0L) + 1L,
                                organizzatoreId = 1L,
                                titolo = dto.titolo,
                                descrizione = dto.descrizione,
                                destinazionePrincipale = dto.destinazionePrincipale,
                                prezzoBase = dto.prezzoBase,
                                durataGiorni = dto.durataGiorni,
                                maxPartecipanti = dto.maxPartecipanti,
                                stato = "ATTIVO"
                            )
                        )
                        Toast.makeText(context, "Itinerario creato con successo!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                )
            }

            composable(CatalogRoutes.CREA_ATTIVITA) {
                CreaAttivitaScreen(
                    onBack = onBack,
                    onSalva = { dto, _ ->
                        mockAttivita.add(
                            SingolaAttivita(
                                id = (mockAttivita.maxOfOrNull { it.id } ?: 0L) + 1L,
                                organizzatoreId = 1L,
                                titolo = dto.titolo,
                                descrizione = dto.descrizione,
                                luogo = dto.luogo,
                                prezzo = dto.prezzo,
                                durataMinuti = dto.durataMinuti,
                                maxPartecipanti = dto.maxPartecipanti
                            )
                        )
                        Toast.makeText(context, "Attività creata con successo!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                )
            }

            composable(CatalogRoutes.MODIFICA_ITINERARIO) {
                CreaItinerarioScreen(
                    itinerarioDaModificare = itinerarioInModifica,
                    onBack = onBack,
                    onSalva = { dto, _ ->
                        val index = mockItinerari.indexOfFirst { it.id == itinerarioInModifica?.id }
                        if (index != -1) {
                            mockItinerari[index] = mockItinerari[index].copy(
                                titolo = dto.titolo,
                                descrizione = dto.descrizione,
                                destinazionePrincipale = dto.destinazionePrincipale,
                                prezzoBase = dto.prezzoBase,
                                durataGiorni = dto.durataGiorni,
                                maxPartecipanti = dto.maxPartecipanti
                            )
                            Toast.makeText(context, "Itinerario aggiornato!", Toast.LENGTH_SHORT).show()
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable(CatalogRoutes.MODIFICA_ATTIVITA) {
                CreaAttivitaScreen(
                    attivitaDaModificare = attivitaInModifica,
                    onBack = onBack,
                    onSalva = { dto, _ ->
                        val index = mockAttivita.indexOfFirst { it.id == attivitaInModifica?.id }
                        if (index != -1) {
                            mockAttivita[index] = mockAttivita[index].copy(
                                titolo = dto.titolo,
                                descrizione = dto.descrizione,
                                luogo = dto.luogo,
                                prezzo = dto.prezzo,
                                durataMinuti = dto.durataMinuti,
                                maxPartecipanti = dto.maxPartecipanti
                            )
                            Toast.makeText(context, "Attività aggiornata!", Toast.LENGTH_SHORT).show()
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable(CatalogRoutes.LE_MIE_OFFERTE) {
                OfferteManagementScreen(
                    isAdmin = false,
                    itinerari = mockItinerari,
                    attivita = mockAttivita,
                    onBack = onBack,
                    onModificaItinerario = { item ->
                        itinerarioInModifica = item
                        navController.navigate(CatalogRoutes.MODIFICA_ITINERARIO)
                    },
                    onEliminaItinerario = { id ->
                        mockItinerari.removeAll { it.id == id }
                        Toast.makeText(context, "Itinerario eliminato!", Toast.LENGTH_SHORT).show()
                    },
                    onModificaAttivita = { item ->
                        attivitaInModifica = item
                        navController.navigate(CatalogRoutes.MODIFICA_ATTIVITA)
                    },
                    onEliminaAttivita = { id ->
                        mockAttivita.removeAll { it.id == id }
                        Toast.makeText(context, "Attività eliminata!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            composable(CatalogRoutes.OFFERTE_ADMIN) {
                OfferteManagementScreen(
                    isAdmin = true,
                    itinerari = mockItinerari,
                    attivita = mockAttivita,
                    onBack = onBack,
                    onModificaItinerario = { item ->
                        itinerarioInModifica = item
                        navController.navigate(CatalogRoutes.MODIFICA_ITINERARIO)
                    },
                    onEliminaItinerario = { id ->
                        mockItinerari.removeAll { it.id == id }
                        Toast.makeText(context, "[ADMIN] Itinerario eliminato!", Toast.LENGTH_SHORT).show()
                    },
                    onModificaAttivita = { item ->
                        attivitaInModifica = item
                        navController.navigate(CatalogRoutes.MODIFICA_ATTIVITA)
                    },
                    onEliminaAttivita = { id ->
                        mockAttivita.removeAll { it.id == id }
                        Toast.makeText(context, "[ADMIN] Attività eliminata!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            composable(CatalogRoutes.GESTIONE_UTENTI_ADMIN) {
                GestioneUtentiAdminScreen(
                    utenti = mockUtenti,
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