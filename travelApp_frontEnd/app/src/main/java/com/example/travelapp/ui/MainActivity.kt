package com.example.travelapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.R
import com.example.travelapp.data.remote.GestoreSessione
import com.example.travelapp.ui.auth.LoginScreen
import com.example.travelapp.ui.auth.RegistrazioneScreen
import com.example.travelapp.ui.components.CaricamentoLottie
import com.example.travelapp.ui.navigation.AppNavGraph
import com.example.travelapp.ui.theme.TravelAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val temaSistema =
                isSystemInDarkTheme()

            var temaScuro by remember {
                mutableStateOf(temaSistema)
            }

            TravelAppTheme(
                darkTheme = temaScuro
            ) {

                AppNavigation(
                    onExit = {
                        finish()
                    },
                    temaSistema = temaSistema,
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
    temaSistema: Boolean,
    onDarkModeChanged: (Boolean) -> Unit
) {

    val context =
        LocalContext.current

    /*
     * Prima di mostrare login/home controlliamo
     * se esiste una sessione salvata valida.
     */
    var schermataCorrente by remember {
        mutableStateOf("avvio")
    }

    /*
     * Dopo una registrazione riuscita conserviamo
     * l'email per precompilare il login.
     */
    var emailAppenaRegistrata by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Cambia ad ogni nuova sessione.
     *
     * Così viene creato un nuovo ProfiloViewModel
     * e non restano dati dell'utente precedente.
     */
    var sessioneId by remember {
        mutableStateOf(0)
    }

    /*
     * Controllo iniziale della sessione.
     */
    LaunchedEffect(Unit) {

        schermataCorrente =
            if (
                GestoreSessione
                    .sessioneRipristinabile(context)
            ) {

                sessioneId++

                "home"

            } else {

                "login"
            }
    }

    when (schermataCorrente) {

        /*
         * ============================================================
         * AVVIO
         * ============================================================
         */

        "avvio" -> {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme
                            .colorScheme
                            .background
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                CaricamentoLottie(
                    dimensione = 120.dp,
                    animazione = R.raw.flight
                )
            }
        }

        /*
         * ============================================================
         * LOGIN
         * ============================================================
         */

        "login" -> {

            LoginScreen(

                onLoginSuccessViaggiatore = {

                    sessioneId++

                    schermataCorrente =
                        "home"
                },

                onLoginSuccessOrganizzatore = {

                    sessioneId++

                    schermataCorrente =
                        "home"
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

        /*
         * ============================================================
         * REGISTRAZIONE
         * ============================================================
         */

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

        /*
         * ============================================================
         * APP
         * ============================================================
         */

        "home" -> {

            AppNavGraph(

                profiloViewModel =
                    viewModel(
                        key =
                            "profilo-$sessioneId"
                    ),

                onExitApp =
                    onExit,

                onLogout = {

                    emailAppenaRegistrata =
                        null

                    schermataCorrente =
                        "login"

                    /*
                     * Dopo il logout non manteniamo
                     * il tema dell'utente precedente.
                     */
                    onDarkModeChanged(
                        temaSistema
                    )
                },

                onDarkModeChanged =
                    onDarkModeChanged
            )
        }
    }
}