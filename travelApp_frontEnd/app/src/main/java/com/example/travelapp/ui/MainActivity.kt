package com.example.travelapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.catalog.*
import com.example.travelapp.ui.navigation.AppNavGraph
import com.example.travelapp.ui.theme.*
import java.math.BigDecimal

enum class TestScreen {
    MENU,
    APP_NAV_GRAPH,
    CREA_ITINERARIO,
    CREA_ATTIVITA,
    MODIFICA_ITINERARIO,
    MODIFICA_ATTIVITA,
    LE_MIE_OFFERTE,
    OFFERTE_ADMIN,
    GESTIONE_UTENTI_ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainTestHub(
                    onExit = { finish() },
                    showToast = { msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTestHub(
    onExit: () -> Unit,
    showToast: (String) -> Unit
) {
    var currentScreen by remember { mutableStateOf(TestScreen.MENU) }
    var previousScreen by remember { mutableStateOf(TestScreen.MENU) }

    var itinerarioInModifica by remember { mutableStateOf<Itinerario?>(null) }
    var attivitaInModifica by remember { mutableStateOf<SingolaAttivita?>(null) }

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

    when (currentScreen) {
        TestScreen.MENU -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Hub Test Generale", fontWeight = FontWeight.Bold, color = TravelTextDark) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(TravelBg)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { currentScreen = TestScreen.APP_NAV_GRAPH },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelBlueDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("0. Avvia App Completa (Profilo / NavGraph)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { currentScreen = TestScreen.CREA_ITINERARIO },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("1. Crea Itinerario (Organizzatore)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { currentScreen = TestScreen.CREA_ATTIVITA },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("2. Crea Attività Singola (Organizzatore)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { currentScreen = TestScreen.LE_MIE_OFFERTE },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("3. Le Mie Offerte (Organizzatore)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { currentScreen = TestScreen.OFFERTE_ADMIN },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("4. Gestione Globale Offerte (Admin)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { currentScreen = TestScreen.GESTIONE_UTENTI_ADMIN },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("5. Gestione Utenti (Admin)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        TestScreen.APP_NAV_GRAPH -> {
            AppNavGraph(onExitApp = { currentScreen = TestScreen.MENU })
        }

        TestScreen.CREA_ITINERARIO -> {
            CreaItinerarioScreen(
                onBack = { currentScreen = TestScreen.MENU },
                onSalva = { dto, uri ->
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
                    showToast("Itinerario creato con successo!")
                    currentScreen = TestScreen.MENU
                }
            )
        }

        TestScreen.CREA_ATTIVITA -> {
            CreaAttivitaScreen(
                onBack = { currentScreen = TestScreen.MENU },
                onSalva = { dto, uri ->
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
                    showToast("Attività creata con successo!")
                    currentScreen = TestScreen.MENU
                }
            )
        }

        TestScreen.MODIFICA_ITINERARIO -> {
            CreaItinerarioScreen(
                itinerarioDaModificare = itinerarioInModifica,
                onBack = { currentScreen = previousScreen },
                onSalva = { dto, uri ->
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
                        showToast("Itinerario aggiornato!")
                    }
                    currentScreen = previousScreen
                }
            )
        }

        TestScreen.MODIFICA_ATTIVITA -> {
            CreaAttivitaScreen(
                attivitaDaModificare = attivitaInModifica,
                onBack = { currentScreen = previousScreen },
                onSalva = { dto, uri ->
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
                        showToast("Attività aggiornata!")
                    }
                    currentScreen = previousScreen
                }
            )
        }

        TestScreen.LE_MIE_OFFERTE -> {
            OfferteManagementScreen(
                isAdmin = false,
                itinerari = mockItinerari,
                attivita = mockAttivita,
                onBack = { currentScreen = TestScreen.MENU },
                onModificaItinerario = { item ->
                    itinerarioInModifica = item
                    previousScreen = TestScreen.LE_MIE_OFFERTE
                    currentScreen = TestScreen.MODIFICA_ITINERARIO
                },
                onEliminaItinerario = { id ->
                    mockItinerari.removeAll { it.id == id }
                    showToast("Itinerario #$id eliminato!")
                },
                onModificaAttivita = { item ->
                    attivitaInModifica = item
                    previousScreen = TestScreen.LE_MIE_OFFERTE
                    currentScreen = TestScreen.MODIFICA_ATTIVITA
                },
                onEliminaAttivita = { id ->
                    mockAttivita.removeAll { it.id == id }
                    showToast("Attività #$id eliminata!")
                }
            )
        }

        TestScreen.OFFERTE_ADMIN -> {
            OfferteManagementScreen(
                isAdmin = true,
                itinerari = mockItinerari,
                attivita = mockAttivita,
                onBack = { currentScreen = TestScreen.MENU },
                onModificaItinerario = { item ->
                    itinerarioInModifica = item
                    previousScreen = TestScreen.OFFERTE_ADMIN
                    currentScreen = TestScreen.MODIFICA_ITINERARIO
                },
                onEliminaItinerario = { id ->
                    mockItinerari.removeAll { it.id == id }
                    showToast("[ADMIN] Itinerario #$id eliminato!")
                },
                onModificaAttivita = { item ->
                    attivitaInModifica = item
                    previousScreen = TestScreen.OFFERTE_ADMIN
                    currentScreen = TestScreen.MODIFICA_ATTIVITA
                },
                onEliminaAttivita = { id ->
                    mockAttivita.removeAll { it.id == id }
                    showToast("[ADMIN] Attività #$id eliminata!")
                }
            )
        }

        TestScreen.GESTIONE_UTENTI_ADMIN -> {
            GestioneUtentiAdminScreen(
                utenti = mockUtenti,
                onBack = { currentScreen = TestScreen.MENU }
            )
        }
    }
}