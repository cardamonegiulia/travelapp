package com.example.travelapp.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccessViaggiatore: () -> Unit,
    onLoginSuccessOrganizzatore: () -> Unit,
    onVaiRegistrazione: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Launcher che apre Keycloak nel browser e aspetta il ritorno
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { intent ->
            viewModel.gestisciRispostaLogin(intent)
        }
    }

    // Reagisce ai cambiamenti di stato
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                if (state.ruolo == "ORGANIZZATORE") {
                    onLoginSuccessOrganizzatore()
                } else {
                    onLoginSuccessViaggiatore()
                }
                viewModel.resetStato()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Titolo
        Text(
            text = "TravelApp",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Scopri il mondo con noi",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Pulsante login
        Button(
            onClick = {
                val intent = viewModel.creaIntentLogin()
                launcher.launch(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = uiState !is AuthUiState.Loading
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Accedi con Keycloak",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link registrazione
        TextButton(onClick = onVaiRegistrazione) {
            Text("Non hai un account? Registrati")
        }

        // Messaggio di errore
        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = (uiState as AuthUiState.Error).messaggio,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}