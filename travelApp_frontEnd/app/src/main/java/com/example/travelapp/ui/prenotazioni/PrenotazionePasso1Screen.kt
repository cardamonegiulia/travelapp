package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrenotazionePasso1Demo() {

    var partecipanti by remember {
        mutableStateOf(1)
    }

    PrenotazionePasso1Screen(
        uiState = BookingUiState(
            numeroPartecipanti = partecipanti
        ),
        onIncrementa = {
            partecipanti++
        },
        onDecrementa = {
            if (partecipanti > 1) {
                partecipanti--
            }
        },
        onContinua = {
            // Per ora non fa nulla.
            // Collegheremo il secondo passo più avanti.
        }
    )
}

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

            Button(
                onClick = onDecrementa,
                enabled = uiState.numeroPartecipanti > 1
            ) {
                Text("-")
            }

            Text(
                text = uiState.numeroPartecipanti.toString(),
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = onIncrementa
            ) {
                Text("+")
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onContinua,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continua al pagamento")
        }
    }
}