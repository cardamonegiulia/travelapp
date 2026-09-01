package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.travelapp.data.remote.dto.dataLeggibile
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.domain.model.StatoPagamento
import com.example.travelapp.domain.model.StatoPrenotazione
import com.example.travelapp.domain.model.TipoPrenotazione
import com.example.travelapp.ui.theme.CompletedBadgeBackground
import com.example.travelapp.ui.theme.CompletedBadgeText
import com.example.travelapp.ui.theme.SuccessGreen
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary

/**
 * Card di un viaggio gia' fatto.
 *
 * L'azione cambia a seconda di cosa dice il server: "Lascia una recensione" finche' il
 * viaggio e' recensibile, "Modifica la tua recensione" quando c'e' gia'. Nessuna delle due
 * compare per una prenotazione di attivita' singola, che non ha un itinerario da recensire.
 */
@Composable
fun ViaggioConclusoCard(
    prenotazione: Prenotazione,
    onClick: () -> Unit,
    onRecensisci: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CompletedBadgeBackground
                ) {
                    Text(
                        text = "CONCLUSO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CompletedBadgeText,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    )
                }

                if (prenotazione.recensioneId != null) {
                    Text(
                        text = "Recensito",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = prenotazione.titolo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = prenotazione.luogo,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            periodo(prenotazione)?.let { quando ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = quando,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Solo i viaggi con itinerario sono recensibili: per un'attivita' singola non
            // c'e' un itinerario a cui agganciare la recensione, e il server lo dice gia'
            // tramite "recensibile"/"recensioneId".
            if (prenotazione.recensibile || prenotazione.recensioneId != null) {
                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onRecensisci,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = if (prenotazione.recensioneId != null) {
                            "Modifica la tua recensione"
                        } else {
                            "Lascia una recensione"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun periodo(prenotazione: Prenotazione): String? {
    val partenza = dataLeggibile(prenotazione.dataInizioViaggio)
    val rientro = dataLeggibile(prenotazione.dataFineViaggio)
    return when {
        partenza != null && rientro != null -> "Dal $partenza al $rientro"
        rientro != null -> "Concluso il $rientro"
        else -> null
    }
}

@Preview(showBackground = true, name = "Viaggio concluso")
@Composable
private fun ViaggioConclusoCardPreview() {
    MaterialTheme {
        ViaggioConclusoCard(
            prenotazione = Prenotazione(
                id = 1L,
                titolo = "Tour della Sila",
                luogo = "Camigliatello",
                numeroPartecipanti = 2,
                prezzoTotale = 240.0,
                statoPrenotazione = StatoPrenotazione.CONFERMATA,
                statoPagamento = StatoPagamento.COMPLETATO,
                tipoPrenotazione = TipoPrenotazione.ITINERARIO,
                dataPrenotazione = "2026-07-01T10:00:00",
                dataInizioViaggio = "2026-08-10T00:00:00",
                dataFineViaggio = "2026-08-14T00:00:00",
                itinerarioId = 3L,
                conclusa = true,
                recensibile = true
            ),
            onClick = {},
            onRecensisci = {}
        )
    }
}
