package com.example.travelapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegistrazioneScreen(
    onRegistrazioneSuccess: () -> Unit,
    onVaiLogin: () -> Unit,
    viewModel: RegistrazioneViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Campi del form
    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ruoloSelezionato by remember { mutableStateOf("VIAGGIATORE") }

    // Naviga al login dopo registrazione riuscita
    LaunchedEffect(uiState) {
        if (uiState is RegistrazioneUiState.Success) {
            onRegistrazioneSuccess()
            viewModel.resetStato()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Crea account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Scelta ruolo — i due pulsanti principali
        Text(
            text = "Scegli come vuoi usare TravelApp",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pulsante VIAGGIATORE
            FilterChip(
                selected = ruoloSelezionato == "VIAGGIATORE",
                onClick = { ruoloSelezionato = "VIAGGIATORE" },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✈️", fontSize = 24.sp)
                        Text("Viaggiatore", fontSize = 12.sp)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
            )

            // Pulsante ORGANIZZATORE
            FilterChip(
                selected = ruoloSelezionato == "ORGANIZZATORE",
                onClick = { ruoloSelezionato = "ORGANIZZATORE" },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗂️", fontSize = 24.sp)
                        Text("Organizzatore", fontSize = 12.sp)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Campo Nome
        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Cognome
        OutlinedTextField(
            value = cognome,
            onValueChange = { cognome = it },
            label = { Text("Cognome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Pulsante registrazione
        Button(
            onClick = {
                viewModel.registra(
                    nome = nome,
                    cognome = cognome,
                    email = email,
                    password = password,
                    ruolo = ruoloSelezionato
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = uiState !is RegistrazioneUiState.Loading &&
                    nome.isNotBlank() && cognome.isNotBlank() &&
                    email.isNotBlank() && password.isNotBlank()
        ) {
            if (uiState is RegistrazioneUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Registrati come $ruoloSelezionato",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onVaiLogin) {
            Text("Hai già un account? Accedi")
        }

        // Messaggio di errore
        if (uiState is RegistrazioneUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = (uiState as RegistrazioneUiState.Error).messaggio,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}