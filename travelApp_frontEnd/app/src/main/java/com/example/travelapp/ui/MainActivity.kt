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
            val temaSistema = isSystemInDarkTheme()
            var temaScuro by remember { mutableStateOf(temaSistema) }

            TravelAppTheme(darkTheme = temaScuro) {
                AppNavigation(
                    onExit = { finish() },
                    temaSistema = temaSistema,
                    onDarkModeChanged = { temaScuro = it }
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

    val context = LocalContext.current

    /*
     * Si parte da "avvio": prima di mostrare qualcosa controlliamo se c'è
     * una sessione salvata da ripristinare, così chi ha già fatto il login
     * non se lo ritrova richiesto a ogni riapertura dell'app.
     */
    var schermataCorrente by remember {
        mutableStateOf("avvio")
    }

    /*
     * Dopo una registrazione riuscita conserviamo l'email
     * per riproporla nella schermata di login.
     */
    var emailAppenaRegistrata by remember {
        mutableStateOf<String?>(null)
    }

    // Key del ProfiloViewModel: cambia a ogni login, così non resta quello (con
    // ruolo e dati) dell'utente precedente dopo un logout.
    var sessioneId by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(Unit) {

        schermataCorrente =
            if (GestoreSessione.sessioneRipristinabile(context)) {
                sessioneId++
                "home"
            } else {
                "login"
            }
    }

    when (schermataCorrente) {

        "avvio" -> {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CaricamentoLottie(
                    dimensione = 120.dp,
                    animazione = R.raw.flight
                )
            }
        }


        "login" -> {

            LoginScreen(
                onLoginSuccessViaggiatore = {
                    sessioneId++
                    schermataCorrente = "home"
                },

                onLoginSuccessOrganizzatore = {
                    sessioneId++
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

            AppNavGraph(
                profiloViewModel = viewModel(key = "profilo-$sessioneId"),
                onExitApp = onExit,
                onLogout = {
                    emailAppenaRegistrata = null
                    schermataCorrente = "login"
                    onDarkModeChanged(temaSistema)
                },
                onDarkModeChanged = onDarkModeChanged
            )
        }
    }
}
