package com.example.travelapp.ui.prenotazioni

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.domain.model.StatoPagamento
import com.example.travelapp.domain.model.StatoPrenotazione
import com.example.travelapp.domain.model.TipoPrenotazione
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.SuccessGreen
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.WarningBackground
import com.example.travelapp.ui.theme.WarningYellow

@Composable
fun PrenotazioneCard(
    prenotazione: Prenotazione,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
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

                TipoPrenotazioneBadge(
                    tipo = prenotazione.tipoPrenotazione
                )

                Text(
                    text = "€${"%.2f".format(prenotazione.prezzoTotale)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = prenotazione.titolo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = prenotazione.luogo,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Partecipanti: ${prenotazione.numeroPartecipanti}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Text(
                    text = prenotazione.dataPrenotazione,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                StatoPrenotazioneBadge(
                    stato = prenotazione.statoPrenotazione
                )

                prenotazione.statoPagamento?.let { statoPagamento ->
                    StatoPagamentoBadge(
                        stato = statoPagamento
                    )
                }
            }
        }
    }
}

@Composable
private fun TipoPrenotazioneBadge(
    tipo: TipoPrenotazione
) {

    val testo = when (tipo) {
        TipoPrenotazione.ITINERARIO ->
            "ITINERARIO"

        TipoPrenotazione.SESSIONE_SINGOLA ->
            "ATTIVITÀ"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {

        Text(
            text = testo,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
        )
    }
}

@Composable
private fun StatoPrenotazioneBadge(
    stato: StatoPrenotazione
) {

    val testo: String
    val coloreSfondo: Color
    val coloreTesto: Color

    when (stato) {

        StatoPrenotazione.CONFERMATA -> {
            testo = "CONFERMATA"
            coloreSfondo = Color(0xFFE8F5EC)
            coloreTesto = SuccessGreen
        }

        StatoPrenotazione.IN_ATTESA -> {
            testo = "IN ATTESA"
            coloreSfondo = WarningBackground
            coloreTesto = WarningYellow
        }

        StatoPrenotazione.CANCELLATA -> {
            testo = "CANCELLATA"
            coloreSfondo = Color(0xFFFFEAEA)
            coloreTesto = ErrorRed
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = coloreSfondo
    ) {

        Text(
            text = testo,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = coloreTesto,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
        )
    }
}

@Composable
private fun StatoPagamentoBadge(
    stato: StatoPagamento
) {

    val testo: String
    val coloreSfondo: Color
    val coloreTesto: Color

    when (stato) {

        StatoPagamento.COMPLETATO -> {
            testo = "PAGATO"
            coloreSfondo = Color(0xFFE8F5EC)
            coloreTesto = SuccessGreen
        }

        StatoPagamento.IN_ATTESA -> {
            testo = "PAGAMENTO IN ATTESA"
            coloreSfondo = WarningBackground
            coloreTesto = WarningYellow
        }

        StatoPagamento.FALLITO -> {
            testo = "PAGAMENTO FALLITO"
            coloreSfondo = Color(0xFFFFEAEA)
            coloreTesto = ErrorRed
        }

        StatoPagamento.RIMBORSATO -> {
            testo = "RIMBORSATO"
            coloreSfondo =
                MaterialTheme.colorScheme.primaryContainer
            coloreTesto =
                MaterialTheme.colorScheme.onPrimaryContainer
        }

        StatoPagamento.ANNULLATO -> {
            testo = "PAGAMENTO ANNULLATO"
            coloreSfondo = Color(0xFFFFEAEA)
            coloreTesto = ErrorRed
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = coloreSfondo
    ) {

        Text(
            text = testo,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = coloreTesto,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
        )
    }
}


@Preview(
    showBackground = true,
    showSystemUi = false,
    name = "Prenotazione Card"
)
@Composable
private fun PrenotazioneCardPreview() {
    PrenotazioneCard(
        prenotazione = Prenotazione(
            id = 1L,
            titolo = "Tour delle Cantine del Chianti",
            luogo = "Toscana",
            numeroPartecipanti = 2,
            prezzoTotale = 240.0,
            statoPrenotazione = StatoPrenotazione.CONFERMATA,
            statoPagamento = StatoPagamento.COMPLETATO,
            tipoPrenotazione = TipoPrenotazione.ITINERARIO,
            dataPrenotazione = "23/08/2026"
        ),
        onClick = {}
    )
}

@Preview(
    showBackground = true,
    name = "Prenotazione cancellata"
)
@Composable
private fun PrenotazioneCancellataPreview() {
    PrenotazioneCard(
        prenotazione = Prenotazione(
            id = 2L,
            titolo = "Escursione Etna",
            luogo = "Catania",
            numeroPartecipanti = 1,
            prezzoTotale = 85.0,
            statoPrenotazione = StatoPrenotazione.CANCELLATA,
            statoPagamento = StatoPagamento.RIMBORSATO,
            tipoPrenotazione = TipoPrenotazione.SESSIONE_SINGOLA,
            dataPrenotazione = "20/08/2026"
        ),
        onClick = {}
    )
}