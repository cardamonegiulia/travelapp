package com.example.travelapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.theme.SuccessGreen
import com.example.travelapp.ui.auth.LoginScreen
import com.example.travelapp.ui.auth.RegistrazioneScreen
import com.example.travelapp.ui.theme.TravelAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TravelAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var schermataCorrente by remember { mutableStateOf("login") }
    var ruoloUtente by remember { mutableStateOf("") }
    var emailAppenaRegistrata by remember { mutableStateOf<String?>(null) }

    when (schermataCorrente) {
        "login" -> LoginScreen(
            onLoginSuccessViaggiatore = {
                ruoloUtente = "VIAGGIATORE"
                schermataCorrente = "home"
            },
            onLoginSuccessOrganizzatore = {
                ruoloUtente = "ORGANIZZATORE"
                schermataCorrente = "home"
            },
            onVaiRegistrazione = {
                emailAppenaRegistrata = null
                schermataCorrente = "registrazione"
            },
            emailPreCompilata = emailAppenaRegistrata
        )

        "registrazione" -> RegistrazioneScreen(
            onRegistrazioneSuccess = { email ->
                emailAppenaRegistrata = email
                schermataCorrente = "login"
            },
            onVaiLogin = {
                schermataCorrente = "login"
            }
        )

        "home" -> {
            // Schermata temporanea per verificare il login
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Login riuscito!",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ruolo: $ruoloUtente",
                    fontSize = 18.sp
                )
            }
        }
    }
}