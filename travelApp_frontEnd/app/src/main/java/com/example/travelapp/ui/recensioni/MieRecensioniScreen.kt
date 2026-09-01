package com.example.travelapp.ui.recensioni

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.travelapp.domain.model.Recensione
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.TravelOrange
import com.example.travelapp.ui.util.formattaData

private const val STELLE_MASSIME = 5

/**
 * Elenco delle recensioni scritte dall'utente.
 *
 * Presentazionale: riceve lo stato gia' pronto e rimanda in su il tocco su una recensione,
 * che porta al form di modifica (la stessa schermata con cui era stata scritta).
 */
@Composable
fun MieRecensioniScreen(
    recensioni: List<Recensione>,
    isLoading: Boolean,
    errore: String?,
    onRiprova: () -> Unit,
    onBack: () -> Unit,
    onRecensioneClick: (Recensione) -> Unit = {},
    modifier: Modifier = Modifier
) {

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "Le mie recensioni",
                onBack = onBack
            )
        }
    ) { innerPadding ->

        when {

            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errore != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Impossibile caricare le recensioni",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    Text(
                        text = errore,
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

            recensioni.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Non hai ancora scritto recensioni",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Quando un viaggio si conclude puoi raccontare com'è andata: " +
                            "le recensioni che scrivi finiscono qui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    items(
                        items = recensioni,
                        key = { it.id }
                    ) { recensione ->

                        MiaRecensioneCard(
                            recensione = recensione,
                            onClick = { onRecensioneClick(recensione) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun MiaRecensioneCard(
    recensione: Recensione,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    // Senza prenotazione non c'e' niente da riaprire: il form di modifica parte da li'.
    val modificabile = recensione.prenotazioneId != null

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (modificabile) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = recensione.titoloItinerario ?: "Viaggio recensito",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                StelleVoto(
                    voto = recensione.votazione
                )

                Text(
                    text = formattaData(recensione.data),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            val commento = recensione.commento

            if (commento != null) {

                Text(
                    text = commento,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

            } else {

                Text(
                    text = "Hai lasciato solo la valutazione, senza commento.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = TextSecondary
                )
            }

            if (modificabile) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Tocca per modificare",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelOrange
                )
            }
        }
    }
}


/** Le stelle di una singola recensione: quante ne ha date l'utente, su cinque. */
@Composable
private fun StelleVoto(
    voto: Int,
    modifier: Modifier = Modifier
) {

    val piene = voto.coerceIn(0, STELLE_MASSIME)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        repeat(STELLE_MASSIME) { indice ->

            Icon(
                imageVector =
                    if (indice < piene) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription =
                    if (indice == 0) "Valutazione $piene su $STELLE_MASSIME" else null,
                tint = if (indice < piene) TravelOrange else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true, name = "Le mie recensioni")
@Composable
private fun MieRecensioniScreenPreview() {

    val recensioni = listOf(
        Recensione(
            id = 1L,
            prenotazioneId = 10L,
            itinerarioId = 5L,
            titoloItinerario = "Tour della Sila",
            autoreId = 1L,
            votazione = 5,
            commento = "Guide molto preparate, rifarei tutto.",
            autore = "Mario Rossi",
            data = "2026-07-12T18:30:00"
        ),
        Recensione(
            id = 2L,
            prenotazioneId = 11L,
            itinerarioId = 6L,
            titoloItinerario = "Weekend a Tropea",
            autoreId = 1L,
            votazione = 3,
            commento = null,
            autore = "Mario Rossi",
            data = "2026-05-02T09:00:00"
        )
    )

    MaterialTheme {
        MieRecensioniScreen(
            recensioni = recensioni,
            isLoading = false,
            errore = null,
            onRiprova = {},
            onBack = {}
        )
    }
}
