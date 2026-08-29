package com.example.travelapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.ui.auth.LoginScreen
import com.example.travelapp.ui.auth.RegistrazioneScreen
import com.example.travelapp.ui.navigation.AppNavGraph
import com.example.travelapp.ui.theme.TravelAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TravelAppTheme {
                AppNavigation(
                    onExit = { finish() }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    onExit: () -> Unit
) {

    var schermataCorrente by remember {
        mutableStateOf("login")
    }

    /*
     * Ruolo ottenuto al login.
     * Viene passato ad AppNavGraph come fallback
     * mentre ProfiloViewModel recupera il profilo reale.
     */
    var ruoloUtente by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Dopo una registrazione riuscita conserviamo l'email
     * per riproporla nella schermata di login.
     */
    var emailAppenaRegistrata by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Cambia a ogni login riuscito.
     *
     * Serve per creare un nuovo ProfiloViewModel per ogni
     * sessione ed evitare di riutilizzare dati/ruolo
     * dell'utente precedente dopo logout e nuovo login.
     */
    var sessioneId by remember {
        mutableStateOf(0)
    }

    when (schermataCorrente) {

        "login" -> {

            LoginScreen(
                onLoginSuccessViaggiatore = {
                    ruoloUtente = "VIAGGIATORE"
                    sessioneId++
                    schermataCorrente = "app"
                },

                onLoginSuccessOrganizzatore = {
                    ruoloUtente = "ORGANIZZATORE"
                    sessioneId++
                    schermataCorrente = "app"
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

        "app" -> {

            AppNavGraph(
                profiloViewModel = viewModel(
                    key = "profilo-$sessioneId"
                ),

                ruoloIniziale = ruoloUtente,

                /*
                 * Se si esce normalmente dalla navigazione
                 * principale, chiudiamo l'Activity.
                 */
                onExitApp = onExit,

                /*
                 * Il logout vero viene gestito dentro AppNavGraph:
                 * lì viene cancellato il token tramite TokenManager.
                 * Qui riportiamo semplicemente l'utente al login.
                 */
                onLogout = {
                    ruoloUtente = null
                    emailAppenaRegistrata = null
                    schermataCorrente = "login"
                }
            )
        }
    }
}