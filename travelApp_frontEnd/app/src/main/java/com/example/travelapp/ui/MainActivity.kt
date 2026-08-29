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
import com.example.travelapp.ui.profilo.ProfiloViewModel
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
     * Dopo una registrazione riuscita conserviamo l'email
     * per riproporla nella schermata di login.
     */
    var emailAppenaRegistrata by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Cambia a ogni login riuscito: usata come key del ProfiloViewModel così
     * Compose ne crea uno nuovo invece di riusare quello (con dentro ancora
     * ruolo e dati) dell'utente precedente — lo ViewModelStore è dell'Activity
     * e sopravvive normalmente a un logout/login.
     */
    var sessioneId by remember {
        mutableStateOf(0)
    }

    when (schermataCorrente) {

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
                }
            )
        }
    }
}
