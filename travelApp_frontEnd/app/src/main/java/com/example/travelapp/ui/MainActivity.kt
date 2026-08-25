package com.example.travelapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.auth.LoginScreen
import com.example.travelapp.ui.auth.RegistrazioneScreen
import com.example.travelapp.ui.catalog.CreaAttivitaScreen
import com.example.travelapp.ui.catalog.CreaItinerarioScreen
import com.example.travelapp.ui.catalog.GestioneUtentiAdminScreen
import com.example.travelapp.ui.catalog.OfferteManagementScreen
import com.example.travelapp.ui.navigation.AppNavGraph
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.ui.theme.TravelBg
import com.example.travelapp.ui.theme.TravelBlue
import com.example.travelapp.ui.theme.TravelBlueDark
import com.example.travelapp.ui.theme.TravelOrange
import com.example.travelapp.ui.theme.TravelTextDark


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
            TravelAppTheme {
                AppNavigation(
                    onExit = { finish() },
                    showToast = { message ->
                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}


@Composable
fun AppNavigation(
    onExit: () -> Unit,
    showToast: (String) -> Unit
) {

    var schermataCorrente by remember {
        mutableStateOf("login")
    }

    /*
     * Dopo una registrazione riuscita conserviamo l'email
     * per riproporla nella schermata di login.
     */
    var emailAppenaRegistrata by remember {
        mutableStateOf<String?>(null)
    }

    when (schermataCorrente) {

        "login" -> {

            LoginScreen(
                onLoginSuccessViaggiatore = {
                    schermataCorrente = "home"
                },

                onLoginSuccessOrganizzatore = {
                    schermataCorrente = "home"
                },

                onVaiRegistrazione = {
                    emailAppenaRegistrata = null
                    schermataCorrente = "registrazione"
                },

                emailPreCompilata = emailAppenaRegistrata
            )
        }


        "registrazione" -> {

            RegistrazioneScreen(
                onRegistrazioneSuccess = { email ->

                    emailAppenaRegistrata = email
                    schermataCorrente = "login"
                },

                onVaiLogin = {
                    schermataCorrente = "login"
                }
            )
        }


        "home" -> {

            MainTestHub(
                onExit = onExit,
                showToast = showToast
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTestHub(
    onExit: () -> Unit,
    showToast: (String) -> Unit
) {

    var currentScreen by remember {
        mutableStateOf(TestScreen.MENU)
    }

    var previousScreen by remember {
        mutableStateOf(TestScreen.MENU)
    }

    var itinerarioInModifica by remember {
        mutableStateOf<Itinerario?>(null)
    }

    var attivitaInModifica by remember {
        mutableStateOf<SingolaAttivita?>(null)
    }


    when (currentScreen) {

        TestScreen.MENU -> {

            Scaffold(
                topBar = {

                    TopAppBar(
                        title = {

                            Text(
                                text = "Hub Test Generale",
                                fontWeight = FontWeight.Bold,
                                color = TravelTextDark
                            )
                        },

                        colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        )
                    )
                }
            ) { padding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(TravelBg)
                        .padding(20.dp),

                    verticalArrangement =
                    Arrangement.spacedBy(12.dp)
                ) {

                    Button(
                        onClick = {
                            currentScreen =
                                TestScreen.APP_NAV_GRAPH
                        },

                        colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                            TravelBlueDark
                        ),

                        shape =
                        RoundedCornerShape(10.dp),

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {

                        Text(
                            text =
                            "0. Avvia App Completa (Profilo / NavGraph)",
                            fontWeight =
                            FontWeight.Bold
                        )
                    }


                    Button(
                        onClick = {
                            currentScreen =
                                TestScreen.CREA_ITINERARIO
                        },

                        colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                            TravelBlue
                        ),

                        shape =
                        RoundedCornerShape(10.dp),

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {

                        Text(
                            text =
                            "1. Crea Itinerario (Organizzatore)",
                            fontWeight =
                            FontWeight.Bold
                        )
                    }


                    Button(
                        onClick = {
                            currentScreen =
                                TestScreen.CREA_ATTIVITA
                        },

                        colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                            TravelBlue
                        ),

                        shape =
                        RoundedCornerShape(10.dp),

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {

                        Text(
                            text =
                            "2. Crea Attività Singola (Organizzatore)",
                            fontWeight =
                            FontWeight.Bold
                        )
                    }


                    Button(
                        onClick = {
                            currentScreen =
                                TestScreen.LE_MIE_OFFERTE
                        },

                        colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                            TravelOrange
                        ),

                        shape =
                        RoundedCornerShape(10.dp),

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {

                        Text(
                            text =
                            "3. Le Mie Offerte (Organizzatore)",
                            fontWeight =
                            FontWeight.Bold
                        )
                    }


                    Button(
                        onClick = {
                            currentScreen =
                                TestScreen.OFFERTE_ADMIN
                        },

                        colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                            TravelOrange
                        ),

                        shape =
                        RoundedCornerShape(10.dp),

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {

                        Text(
                            text =
                            "4. Gestione Globale Offerte (Admin)",
                            fontWeight =
                            FontWeight.Bold
                        )
                    }


                    Button(
                        onClick = {
                            currentScreen =
                                TestScreen.GESTIONE_UTENTI_ADMIN
                        },

                        colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                            Color(0xFF0F172A)
                        ),

                        shape =
                        RoundedCornerShape(10.dp),

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {

                        Text(
                            text =
                            "5. Gestione Utenti (Admin)",
                            fontWeight =
                            FontWeight.Bold
                        )
                    }
                }
            }
        }


        /*
         * APP COMPLETA
         */
        TestScreen.APP_NAV_GRAPH -> {

            AppNavGraph(
                onExitApp = {
                    currentScreen =
                        TestScreen.MENU
                }
            )
        }


        /*
         * CREAZIONE ITINERARIO
         */
        TestScreen.CREA_ITINERARIO -> {

            CreaItinerarioScreen(
                onBack = {
                    currentScreen =
                        TestScreen.MENU
                }
            )
        }


        /*
         * CREAZIONE ATTIVITÀ
         */
        TestScreen.CREA_ATTIVITA -> {

            CreaAttivitaScreen(
                onBack = {
                    currentScreen =
                        TestScreen.MENU
                }
            )
        }


        /*
         * MODIFICA ITINERARIO
         */
        TestScreen.MODIFICA_ITINERARIO -> {

            CreaItinerarioScreen(
                itinerarioDaModificare =
                itinerarioInModifica,

                onBack = {
                    currentScreen =
                        previousScreen
                }
            )
        }


        /*
         * MODIFICA ATTIVITÀ
         */
        TestScreen.MODIFICA_ATTIVITA -> {

            CreaAttivitaScreen(
                attivitaDaModificare =
                attivitaInModifica,

                onBack = {
                    currentScreen =
                        previousScreen
                }
            )
        }


        /*
         * OFFERTE ORGANIZZATORE
         *
         * Ora la schermata recupera i dati tramite
         * la propria logica/API, quindi non passiamo più
         * le vecchie liste mock.
         */
        TestScreen.LE_MIE_OFFERTE -> {

            OfferteManagementScreen(
                isAdmin = false,

                onBack = {
                    currentScreen =
                        TestScreen.MENU
                },

                onModificaItinerario = { item ->

                    itinerarioInModifica = item

                    previousScreen =
                        TestScreen.LE_MIE_OFFERTE

                    currentScreen =
                        TestScreen.MODIFICA_ITINERARIO
                },

                onModificaAttivita = { item ->

                    attivitaInModifica = item

                    previousScreen =
                        TestScreen.LE_MIE_OFFERTE

                    currentScreen =
                        TestScreen.MODIFICA_ATTIVITA
                }
            )
        }


        /*
         * OFFERTE ADMIN
         */
        TestScreen.OFFERTE_ADMIN -> {

            OfferteManagementScreen(
                isAdmin = true,

                onBack = {
                    currentScreen =
                        TestScreen.MENU
                }
            )
        }


        /*
         * GESTIONE UTENTI ADMIN
         */
        TestScreen.GESTIONE_UTENTI_ADMIN -> {

            GestioneUtentiAdminScreen(
                onBack = {
                    currentScreen =
                        TestScreen.MENU
                }
            )
        }
    }
}