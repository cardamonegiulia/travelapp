package com.example.travelapp.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelapp.domain.model.PartenzaOrganizzatore
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.ChevronGrey
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.TravelBlue
import com.example.travelapp.ui.util.formattaIntervalloDate

/**
 * Le partenze di un itinerario dell'organizzatore, dalla piu' vicina.
 *
 * Presentazionale: le partenze gia' concluse non arrivano nemmeno, perche' e' il backend a
 * escluderle. Toccando un periodo si va all'elenco di chi lo ha prenotato.
 */
@Composable
fun PartenzeItinerarioScreen(
    state: PartenzeUiState,
    onBack: () -> Unit,
    onPartenzaClick: (PartenzaOrganizzatore) -> Unit,
    onRiprova: () -> Unit,
    modifier: Modifier = Modifier
) {

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = state.titoloItinerario.ifBlank { "Partenze" },
                onBack = onBack
            )
        }
    ) { innerPadding ->

        when {

            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errore != null -> {
                MessaggioErrore(
                    titolo = "Impossibile caricare le partenze",
                    dettaglio = state.errore,
                    onRiprova = onRiprova,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            state.partenze.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Non ci sono partenze in programma per questo itinerario.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        Text(
                            text = "Scegli un periodo per vedere chi si è prenotato.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    items(
                        state.partenze,
                        key = { it.disponibilitaId }
                    ) { partenza ->
                        PartenzaCard(
                            partenza = partenza,
                            onClick = { onPartenzaClick(partenza) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PartenzaCard(
    partenza: PartenzaOrganizzatore,
    onClick: () -> Unit
) {

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = formattaIntervalloDate(partenza.dataInizio, partenza.dataFine),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (partenza.senzaPrenotazioni) {
                        "Nessuna prenotazione"
                    } else {
                        etichettaPrenotazioni(partenza.numeroPrenotazioni) +
                                " · ${partenza.partecipantiTotali} partecipanti"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (partenza.senzaPrenotazioni) TextSecondary else TravelBlue,
                    fontWeight = if (partenza.senzaPrenotazioni) {
                        FontWeight.Normal
                    } else {
                        FontWeight.SemiBold
                    }
                )

                partenza.postiDisponibili?.let { posti ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$posti posti ancora liberi",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ChevronGrey,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


/** "1 prenotazione" e non "1 prenotazioni". */
internal fun etichettaPrenotazioni(numero: Long): String =
    if (numero == 1L) "1 prenotazione" else "$numero prenotazioni"


/**
 * Errore a tutta schermata con il pulsante per riprovare: identico nelle due schermate
 * dell'organizzatore, quindi vive qui una volta sola.
 */
@Composable
internal fun MessaggioErrore(
    titolo: String,
    dettaglio: String,
    onRiprova: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = titolo,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Text(
            text = dettaglio,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = onRiprova,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Riprova")
        }
    }
}
