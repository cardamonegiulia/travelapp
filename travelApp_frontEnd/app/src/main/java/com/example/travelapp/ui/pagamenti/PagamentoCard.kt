package com.example.travelapp.ui.pagamenti

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
import androidx.compose.ui.unit.dp
import com.example.travelapp.domain.model.Pagamento
import com.example.travelapp.domain.model.StatoPagamento
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.SuccessGreen
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.WarningBackground
import com.example.travelapp.ui.theme.WarningYellow

@Composable
fun PagamentoCard(
    pagamento: Pagamento,
    modifier: Modifier = Modifier
) {

    Card(
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

                Text(
                    text = "Pagamento #${pagamento.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "€${"%.2f".format(pagamento.importo)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Prenotazione #${pagamento.prenotazioneId}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            pagamento.dataPagamento?.let { data ->
                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = data,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            StatoPagamentoBadge(
                stato = pagamento.statoPagamento
            )
        }
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
            testo = "PAGAMENTO COMPLETATO"
            coloreSfondo = Color(0xFFE8F5EC)
            coloreTesto = SuccessGreen
        }

        StatoPagamento.IN_ATTESA -> {
            testo = "IN ATTESA"
            coloreSfondo = WarningBackground
            coloreTesto = WarningYellow
        }

        StatoPagamento.FALLITO -> {
            testo = "FALLITO"
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
            testo = "ANNULLATO"
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