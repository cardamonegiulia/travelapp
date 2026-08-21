package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ExtraUi(
    val id: Long,
    val nome: String,
    val prezzo: Double
)

@Composable
fun PrenotazionePasso1Demo() {

    var partecipanti by remember {
        mutableStateOf(1)
    }

    var extraSelezionati by remember {
        mutableStateOf(emptyList<Long>())
    }

    val extraDisponibili = listOf(
        ExtraUi(
            id = 1L,
            nome = "Cena tipica",
            prezzo = 25.0
        ),
        ExtraUi(
            id = 2L,
            nome = "Degustazione",
            prezzo = 15.0
        )
    )

    val prezzoBaseUnitario = 100.0

    val prezzoBase =
        prezzoBaseUnitario * partecipanti

    val prezzoExtra =
        extraDisponibili
            .filter { it.id in extraSelezionati }
            .sumOf { it.prezzo * partecipanti }

    val totale =
        prezzoBase + prezzoExtra

    PrenotazionePasso1Screen(
        uiState = BookingUiState(
            numeroPartecipanti = partecipanti,
            attivitaExtraIds = extraSelezionati,
            prezzoBase = prezzoBase,
            prezzoExtra = prezzoExtra,
            prezzoTotaleVisualizzato = totale
        ),
        extraDisponibili = extraDisponibili,
        onIncrementa = {
            partecipanti++
        },
        onDecrementa = {
            if (partecipanti > 1) {
                partecipanti--
            }
        },
        onToggleExtra = { id ->
            extraSelezionati =
                if (id in extraSelezionati) {
                    extraSelezionati - id
                } else {
                    extraSelezionati + id
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
    extraDisponibili: List<ExtraUi>,
    onIncrementa: () -> Unit,
    onDecrementa: () -> Unit,
    onToggleExtra: (Long) -> Unit,
    onContinua: () -> Unit
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

        Text(
            text = "Extra consigliati",
            style = MaterialTheme.typography.titleMedium
        )

        extraDisponibili.forEach { extra ->

            val selezionato =
                extra.id in uiState.attivitaExtraIds

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = selezionato,
                    onCheckedChange = {
                        onToggleExtra(extra.id)
                    }
                )

                Text(
                    text = "${extra.nome} - €${"%.2f".format(extra.prezzo)}"
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "Subtotale: €${"%.2f".format(uiState.prezzoBase)}"
        )

        Text(
            text = "Extra: €${"%.2f".format(uiState.prezzoExtra)}"
        )

        Text(
            text = "Totale: €${"%.2f".format(uiState.prezzoTotaleVisualizzato)}",
            style = MaterialTheme.typography.titleLarge
        )

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