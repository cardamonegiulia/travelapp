package com.example.travelapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.TravelOrange
import kotlin.math.roundToInt

private const val STELLE_MASSIME = 5

/**
 * Stelle in sola lettura.
 *
 * Con [media] nulla non disegna zero stelle piene ma la dicitura "Nessuna recensione":
 * zero non e' un voto assegnabile, e cinque stelle vuote si leggerebbero come una
 * stroncatura invece che come un'assenza di giudizi.
 */
@Composable
fun StelleValutazione(
    media: Double?,
    numeroRecensioni: Long = 0,
    modifier: Modifier = Modifier,
    dimensione: Dp = 16.dp,
    mostraConteggio: Boolean = true,
    testoQuandoAssente: String = "Nessuna recensione"
) {
    if (media == null || numeroRecensioni <= 0L) {
        Text(
            text = testoQuandoAssente,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = modifier
        )
        return
    }

    val piene = media.roundToInt().coerceIn(0, STELLE_MASSIME)

    Row(
        modifier = modifier.semantics {
            contentDescription = "Valutazione media $piene su $STELLE_MASSIME, $numeroRecensioni recensioni"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(STELLE_MASSIME) { indice ->
            Icon(
                imageVector = if (indice < piene) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (indice < piene) TravelOrange else TextSecondary,
                modifier = Modifier.size(dimensione)
            )
        }

        if (mostraConteggio) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${"%.1f".format(media)} ($numeroRecensioni)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
    }
}

/**
 * Selettore a stelle del form recensione.
 *
 * [valore] a 0 significa "non ancora scelto": e' lo stato iniziale, e il pulsante di invio
 * resta disabilitato finche' resta tale, perche' la valutazione e' obbligatoria.
 */
@Composable
fun SelettoreStelle(
    valore: Int,
    onValoreCambiato: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dimensione: Dp = 40.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..STELLE_MASSIME).forEach { stella ->
            Icon(
                imageVector = if (stella <= valore) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "$stella stelle su $STELLE_MASSIME",
                tint = if (stella <= valore) TravelOrange else TextSecondary,
                modifier = Modifier
                    .size(dimensione)
                    .clickable { onValoreCambiato(stella) }
            )
        }
    }
}

@Preview(showBackground = true, name = "Stelle")
@Composable
private fun StellePreview() {
    MaterialTheme {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StelleValutazione(media = 4.2, numeroRecensioni = 12)
            Spacer(Modifier.width(16.dp))
            StelleValutazione(media = null)
        }
    }
}

@Preview(showBackground = true, name = "Selettore stelle")
@Composable
private fun SelettoreStellePreview() {
    MaterialTheme {
        SelettoreStelle(valore = 3, onValoreCambiato = {})
    }
}
