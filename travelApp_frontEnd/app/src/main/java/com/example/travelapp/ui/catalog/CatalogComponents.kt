package com.example.travelapp.ui.catalog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.travelapp.data.remote.dto.dataLeggibile
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.components.AuthedAsyncImage
import com.example.travelapp.ui.components.StelleValutazione
import com.example.travelapp.ui.theme.WarningBackground
import com.example.travelapp.ui.theme.WarningYellow
@Composable
fun ItinerarioCard(
    itinerario: Itinerario,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            AuthedAsyncImage(
                url = itinerario.immagini
                    .firstOrNull()
                    ?.url,
                contentDescription = itinerario.titolo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    ),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "ITINERARIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    itinerario.prezzoBase?.let { prezzo ->
                        Text(
                            text = "€$prezzo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                StelleValutazione(
                    media = itinerario.mediaVoti,
                    numeroRecensioni = itinerario.numeroRecensioni
                )
                Spacer(
                    modifier = Modifier.height(10.dp)
                )
                Text(
                    text = itinerario.titolo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                itinerario.descrizione?.let { descrizione ->
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                    Text(
                        text = descrizione,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (
                    itinerario.destinazionePrincipale != null ||
                    itinerario.durataGiorni != null
                ) {
                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itinerario.destinazionePrincipale
                            ?.let { destinazione ->
                                Text(
                                    text = "📍 $destinazione",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        itinerario.durataGiorni
                            ?.let { durata ->
                                Text(
                                    text = "📅 $durata giorni",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                    }
                    val partenza =
                        dataLeggibile(itinerario.dataInizio)
                    val rientro =
                        dataLeggibile(itinerario.dataFine)
                    if (
                        partenza != null &&
                        rientro != null
                    ) {
                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )
                        Text(
                            text = "Dal $partenza al $rientro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!itinerario.dateDisponibili) {
                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                    EtichettaNessunaData()
                }
            }
        }
    }
}
@Composable
fun SingolaAttivitaCard(
    attivita: SingolaAttivita,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            AuthedAsyncImage(
                url = attivita.immagini
                    .firstOrNull()
                    ?.url,
                contentDescription = attivita.titolo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    ),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "ATTIVITÀ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    attivita.prezzo?.let { prezzo ->
                        Text(
                            text = "€$prezzo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(
                    modifier = Modifier.height(10.dp)
                )
                Text(
                    text = attivita.titolo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                attivita.descrizione?.let { descrizione ->
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                    Text(
                        text = descrizione,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (
                    attivita.luogo != null ||
                    attivita.durataMinuti != null
                ) {
                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        attivita.luogo
                            ?.let { luogo ->
                                Text(
                                    text = "📍 $luogo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        attivita.durataMinuti
                            ?.let { durata ->
                                Text(
                                    text = "⏱️ $durata min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                    }
                }
            }
        }
    }
}
@Composable
fun EtichettaNessunaData(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = WarningBackground,
        modifier = modifier
    ) {
        Text(
            text = "Nessuna data disponibile",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = WarningYellow,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
        )
    }
}
