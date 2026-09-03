package com.example.travelapp.ui.recensioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.components.SelettoreStelle
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.TravelOrange

private const val MAX_CARATTERI_COMMENTO = 2000


@Composable
fun RecensioneScreen(
    uiState: RecensioneUiState,
    onBack: () -> Unit,
    onVotazioneCambiata: (Int) -> Unit,
    onCommentoCambiato: (String) -> Unit,
    onSalva: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = if (uiState.isModifica) "Modifica recensione" else "Lascia una recensione",
                onBack = onBack
            )
        }
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Il tuo viaggio",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = uiState.titoloViaggio.ifBlank { "Viaggio concluso" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quante stelle gli dai?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "La valutazione è obbligatoria.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                SelettoreStelle(
                    valore = uiState.votazione,
                    onValoreCambiato = onVotazioneCambiata,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Vuoi raccontare com'è andata?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Il commento è facoltativo: puoi lasciare anche solo le stelle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = uiState.commento,
                    onValueChange = { testo ->
                        if (testo.length <= MAX_CARATTERI_COMMENTO) {
                            onCommentoCambiato(testo)
                        }
                    },
                    placeholder = { Text("Il tuo commento (facoltativo)") },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                Text(
                    text = "${uiState.commento.length}/$MAX_CARATTERI_COMMENTO",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            uiState.errore?.let { messaggio ->
                Text(
                    text = messaggio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed
                )
            }

            Button(
                onClick = onSalva,
                enabled = uiState.puoSalvare,
                colors = ButtonDefaults.buttonColors(containerColor = TravelOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isSalvataggio) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        text = if (uiState.isModifica) "Aggiorna recensione" else "Pubblica recensione",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Recensione")
@Composable
private fun RecensioneScreenPreview() {
    MaterialTheme {
        RecensioneScreen(
            uiState = RecensioneUiState(
                prenotazioneId = 1L,
                titoloViaggio = "Tour della Sila",
                votazione = 4,
                commento = "Guide molto preparate."
            ),
            onBack = {},
            onVotazioneCambiata = {},
            onCommentoCambiato = {},
            onSalva = {}
        )
    }
}
