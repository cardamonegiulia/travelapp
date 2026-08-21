package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrenotazionePasso2Screen(
    uiState: BookingUiState,
    onMetodoPagamentoSelezionato: (MetodoPagamentoUi) -> Unit,
    onConfermaEPaga: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 24.dp,
                bottom = 40.dp
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            text = "Pagamento",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Riepilogo",
            style = MaterialTheme.typography.titleMedium
        )

        if (uiState.titolo.isNotBlank()) {
            Text(uiState.titolo)
        }

        if (uiState.luogo.isNotBlank()) {
            Text(uiState.luogo)
        }

        Text(
            text = "Partecipanti: ${uiState.numeroPartecipanti}"
        )

        HorizontalDivider()

        Text(
            text = "Totale: €${"%.2f".format(uiState.prezzoTotaleVisualizzato)}",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Metodo di pagamento",
            style = MaterialTheme.typography.titleMedium
        )

        MetodoPagamentoRow(
            testo = "Carta di credito",
            selezionato =
                uiState.metodoPagamento == MetodoPagamentoUi.CARTA_CREDITO,
            onClick = {
                onMetodoPagamentoSelezionato(
                    MetodoPagamentoUi.CARTA_CREDITO
                )
            }
        )

        MetodoPagamentoRow(
            testo = "PayPal",
            selezionato =
                uiState.metodoPagamento == MetodoPagamentoUi.PAYPAL,
            onClick = {
                onMetodoPagamentoSelezionato(
                    MetodoPagamentoUi.PAYPAL
                )
            }
        )

        MetodoPagamentoRow(
            testo = "Bonifico bancario",
            selezionato =
                uiState.metodoPagamento == MetodoPagamentoUi.BONIFICO,
            onClick = {
                onMetodoPagamentoSelezionato(
                    MetodoPagamentoUi.BONIFICO
                )
            }
        )

        Text(
            text = "Il pagamento è simulato. Nessun addebito reale verrà effettuato."
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onConfermaEPaga,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.isLoading) {
                    "Pagamento in corso..."
                } else {
                    "Conferma e paga"
                }
            )
        }
    }
}

@Composable
private fun MetodoPagamentoRow(
    testo: String,
    selezionato: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selezionato,
            onClick = onClick
        )

        Text(text = testo)
    }
}