package com.example.travelapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
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

    var ruoloUtente by remember {
        mutableStateOf<String?>(null)
    }

    var emailAppenaRegistrata by remember {
        mutableStateOf<String?>(null)
    }

    when (schermataCorrente) {
        "login" -> {
            LoginScreen(
                onLoginSuccessViaggiatore = {
                    ruoloUtente = "VIAGGIATORE"
                    schermataCorrente = "app"
                },
                onLoginSuccessOrganizzatore = {
                    ruoloUtente = "ORGANIZZATORE"
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
                ruoloIniziale = ruoloUtente,
                onExitApp = {
                    schermataCorrente = "login"
                }
            )
        }
    }
}