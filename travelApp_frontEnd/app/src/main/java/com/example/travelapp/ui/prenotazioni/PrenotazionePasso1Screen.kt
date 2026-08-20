package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrenotazionePasso1Screen(
    uiState: BookingUiState,
    onIncrementa: () -> Unit,
    onDecrementa: () -> Unit,
    onContinua: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Text(
            text = "Prenotazione",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Numero partecipanti",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Button(onClick = onDecrementa) {
                Text("-")
            }

            Text(
                text = uiState.numeroPartecipanti.toString(),
                style = MaterialTheme.typography.headlineSmall
            )

            Button(onClick = onIncrementa) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinua,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continua al pagamento")
        }
    }
}