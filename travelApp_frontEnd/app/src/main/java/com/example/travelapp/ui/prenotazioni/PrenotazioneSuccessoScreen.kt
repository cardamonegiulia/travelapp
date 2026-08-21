package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrenotazioneSuccessoScreen(
    uiState: BookingUiState,
    onFine: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Prenotazione confermata!",
            style = MaterialTheme.typography.headlineMedium
        )

        uiState.prenotazioneCreata?.let { prenotazione ->
            Text(
                text = prenotazione.titolo,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "Totale: €${"%.2f".format(prenotazione.prezzoTotale)}",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = onFine,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text("Torna alle mie prenotazioni")
        }
    }
}