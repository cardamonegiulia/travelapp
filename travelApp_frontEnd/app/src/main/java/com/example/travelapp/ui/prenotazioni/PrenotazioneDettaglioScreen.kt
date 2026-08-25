package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.domain.model.StatoPrenotazione
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary

@Composable
fun PrenotazioneDettaglioScreen(
    prenotazione: Prenotazione,
    onBack: () -> Unit,
    onAnnulla: () -> Unit,
    isLoading: Boolean = false
) {

    val annullabile =
        prenotazione.statoPrenotazione != StatoPrenotazione.CANCELLATA

    Scaffold(
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "Dettaglio prenotazione",
                onBack = onBack
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceWhite
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = prenotazione.titolo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = prenotazione.luogo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    HorizontalDivider()

                    DettaglioRiga(
                        etichetta = "Tipo",
                        valore = when (prenotazione.tipoPrenotazione) {
                            com.example.travelapp.domain.model.TipoPrenotazione.ITINERARIO ->
                                "Itinerario"
                            com.example.travelapp.domain.model.TipoPrenotazione.SESSIONE_SINGOLA ->
                                "Attività singola"
                        }
                    )

                    DettaglioRiga(
                        etichetta = "Partecipanti",
                        valore = prenotazione.numeroPartecipanti.toString()
                    )

                    DettaglioRiga(
                        etichetta = "Data prenotazione",
                        valore = prenotazione.dataPrenotazione
                    )

                    DettaglioRiga(
                        etichetta = "Stato prenotazione",
                        valore = prenotazione.statoPrenotazione.name
                    )

                    DettaglioRiga(
                        etichetta = "Stato pagamento",
                        valore = prenotazione.statoPagamento?.name
                            ?: "Non disponibile"
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Totale",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "€${"%.2f".format(prenotazione.prezzoTotale)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (annullabile) {

                Button(
                    onClick = onAnnulla,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {

                    Text(
                        text = if (isLoading) {
                            "Annullamento in corso..."
                        } else {
                            "Annulla prenotazione"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DettaglioRiga(
    etichetta: String,
    valore: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = etichetta,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Text(
            text = valore,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}