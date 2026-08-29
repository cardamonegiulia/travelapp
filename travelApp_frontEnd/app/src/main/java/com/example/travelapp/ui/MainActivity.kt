package com.example.travelapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
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

            val temaSistema = isSystemInDarkTheme()

            var temaScuro by remember {
                mutableStateOf(temaSistema)
            }

            TravelAppTheme(
                darkTheme = temaScuro
            ) {
                AppNavigation(
                    onExit = { finish() },
                    onDarkModeChanged = {
                        temaScuro = it
                    }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    onExit: () -> Unit,
    onDarkModeChanged: (Boolean) -> Unit
) {

    var schermataCorrente by remember {
        mutableStateOf("login")
    }

    /*
     * Ruolo ottenuto durante il login.
     * AppNavGraph lo utilizza come fallback mentre
     * viene caricato il profilo reale dal backend.
     */
    var ruoloUtente by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Dopo una registrazione riuscita conserviamo
     * l'email per precompilarla nel login.
     */
    var emailAppenaRegistrata by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Cambia dopo ogni login riuscito.
     *
     * In questo modo viene creato un nuovo
     * ProfiloViewModel per ogni sessione e non
     * rimangono dati dell'utente precedente.
     */
    var sessioneId by remember {
        mutableStateOf(0)
    }

    when (schermataCorrente) {

        "login" -> {

            LoginScreen(

                onLoginSuccessViaggiatore = {

                    ruoloUtente =
                        "VIAGGIATORE"

                    sessioneId++

                    schermataCorrente =
                        "app"
                },

                onLoginSuccessOrganizzatore = {

                    ruoloUtente =
                        "ORGANIZZATORE"

                    sessioneId++

                    schermataCorrente =
                        "app"
                },

                onVaiRegistrazione = {

                    emailAppenaRegistrata =
                        null

                    schermataCorrente =
                        "registrazione"
                },

                emailPreCompilata =
                    emailAppenaRegistrata
            )
        }


        "registrazione" -> {

            RegistrazioneScreen(

                onRegistrazioneSuccess = { email ->

                    emailAppenaRegistrata =
                        email

                    schermataCorrente =
                        "login"
                },

                onVaiLogin = {

                    schermataCorrente =
                        "login"
                }
            )
        }


        "app" -> {

            AppNavGraph(

                profiloViewModel =
                    viewModel(
                        key =
                            "profilo-$sessioneId"
                    ),

                ruoloIniziale =
                    ruoloUtente,

                /*
                 * Fine della navigazione:
                 * chiude l'Activity.
                 */
                onExitApp =
                    onExit,

                /*
                 * Il token viene cancellato da
                 * AppNavGraph tramite TokenManager.
                 * Qui riportiamo semplicemente
                 * l'app alla schermata di login.
                 */
                onLogout = {

                    ruoloUtente =
                        null

                    emailAppenaRegistrata =
                        null

                    schermataCorrente =
                        "login"
                },

                /*
                 * Riceve dal profilo la preferenza
                 * Light/Dark e la propaga al tema
                 * principale dell'app.
                 */
                onDarkModeChanged =
                    onDarkModeChanged
            )
        }
    }
}