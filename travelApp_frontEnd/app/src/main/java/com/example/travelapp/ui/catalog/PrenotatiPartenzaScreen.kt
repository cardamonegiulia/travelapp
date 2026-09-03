package com.example.travelapp.ui.catalog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelapp.domain.model.PartenzaOrganizzatore
import com.example.travelapp.domain.model.PrenotatoPartenza
import com.example.travelapp.domain.model.StatoPagamento
import com.example.travelapp.domain.model.StatoPrenotazione
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.SuccessGreen
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.TravelBlue
import com.example.travelapp.ui.theme.WarningBackground
import com.example.travelapp.ui.theme.WarningYellow
import com.example.travelapp.ui.util.formattaData
import com.example.travelapp.ui.util.formattaIntervalloDate
@Composable
fun PrenotatiPartenzaScreen(
    state: PrenotatiUiState,
    onBack: () -> Unit,
    onRiprova: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "Prenotati",
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
                    titolo = "Impossibile caricare i prenotati",
                    dettaglio = state.errore,
                    onRiprova = onRiprova,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.partenza?.let { partenza ->
                        item {
                            RiepilogoPartenza(partenza)
                        }
                    }
                    if (state.prenotati.isEmpty()) {
                        item {
                            Text(
                                text = "Nessuno si è ancora prenotato per questo periodo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                    } else {
                        items(
                            state.prenotati,
                            key = { it.prenotazioneId }
                        ) { prenotato ->
                            PrenotatoCard(prenotato)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun RiepilogoPartenza(
    partenza: PartenzaOrganizzatore
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TravelBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formattaIntervalloDate(partenza.dataInizio, partenza.dataFine),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = etichettaPrenotazioni(partenza.numeroPrenotazioni) +
                        " · ${partenza.partecipantiTotali} partecipanti",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
@Composable
private fun PrenotatoCard(
    prenotato: PrenotatoPartenza
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = prenotato.nomeCompleto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "€ %.2f".format(prenotato.prezzoTotale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelBlue
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = etichettaPartecipanti(prenotato.numeroPartecipanti) +
                        " · prenotato il ${formattaData(prenotato.dataPrenotazione)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(
                    testo = testoStatoPrenotazione(prenotato.statoPrenotazione),
                    coloreSfondo = sfondoStatoPrenotazione(prenotato.statoPrenotazione),
                    coloreTesto = testoColoreStatoPrenotazione(prenotato.statoPrenotazione)
                )
                prenotato.statoPagamento?.let { stato ->
                    Badge(
                        testo = testoStatoPagamento(stato),
                        coloreSfondo = sfondoStatoPagamento(stato),
                        coloreTesto = testoColoreStatoPagamento(stato)
                    )
                }
            }
        }
    }
}
@Composable
private fun Badge(
    testo: String,
    coloreSfondo: Color,
    coloreTesto: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = coloreSfondo
    ) {
        Text(
            text = testo,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = coloreTesto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
private fun etichettaPartecipanti(numero: Int): String =
    if (numero == 1) "1 partecipante" else "$numero partecipanti"
private val VerdeChiaro = Color(0xFFE8F5EC)
private val RossoChiaro = Color(0xFFFFEAEA)
private fun testoStatoPrenotazione(stato: StatoPrenotazione): String =
    when (stato) {
        StatoPrenotazione.CONFERMATA -> "CONFERMATA"
        StatoPrenotazione.IN_ATTESA -> "IN ATTESA"
        StatoPrenotazione.CANCELLATA -> "CANCELLATA"
    }
private fun sfondoStatoPrenotazione(stato: StatoPrenotazione): Color =
    when (stato) {
        StatoPrenotazione.CONFERMATA -> VerdeChiaro
        StatoPrenotazione.IN_ATTESA -> WarningBackground
        StatoPrenotazione.CANCELLATA -> RossoChiaro
    }
private fun testoColoreStatoPrenotazione(stato: StatoPrenotazione): Color =
    when (stato) {
        StatoPrenotazione.CONFERMATA -> SuccessGreen
        StatoPrenotazione.IN_ATTESA -> WarningYellow
        StatoPrenotazione.CANCELLATA -> ErrorRed
    }
private fun testoStatoPagamento(stato: StatoPagamento): String =
    when (stato) {
        StatoPagamento.COMPLETATO -> "PAGATO"
        StatoPagamento.IN_ATTESA -> "PAGAMENTO IN ATTESA"
        StatoPagamento.FALLITO -> "PAGAMENTO FALLITO"
        StatoPagamento.RIMBORSATO -> "RIMBORSATO"
        StatoPagamento.ANNULLATO -> "PAGAMENTO ANNULLATO"
    }
private fun sfondoStatoPagamento(stato: StatoPagamento): Color =
    when (stato) {
        StatoPagamento.COMPLETATO -> VerdeChiaro
        StatoPagamento.IN_ATTESA -> WarningBackground
        StatoPagamento.FALLITO, StatoPagamento.ANNULLATO -> RossoChiaro
        StatoPagamento.RIMBORSATO -> VerdeChiaro
    }
private fun testoColoreStatoPagamento(stato: StatoPagamento): Color =
    when (stato) {
        StatoPagamento.COMPLETATO -> SuccessGreen
        StatoPagamento.IN_ATTESA -> WarningYellow
        StatoPagamento.FALLITO, StatoPagamento.ANNULLATO -> ErrorRed
        StatoPagamento.RIMBORSATO -> TravelBlue
    }
