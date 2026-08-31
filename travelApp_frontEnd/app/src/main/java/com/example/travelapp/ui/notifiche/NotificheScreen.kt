package com.example.travelapp.ui.notifiche

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.travelapp.data.remote.dto.dataLeggibile
import com.example.travelapp.domain.model.Notifica
import com.example.travelapp.domain.model.TipoNotifica
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.TravelOrange

/**
 * Elenco delle notifiche in-app.
 *
 * L'invito a recensire non e' un messaggio da leggere e basta: porta con se' l'azione
 * diretta verso il form della recensione di quel viaggio.
 */
@Composable
fun NotificheScreen(
    uiState: NotificheUiState,
    onBack: () -> Unit,
    onApriRecensione: (Notifica) -> Unit,
    onRiprova: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AppTopBar(title = "Notifiche", onBack = onBack) }
    ) { innerPadding ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errore != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Impossibile caricare le notifiche",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = uiState.errore,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(onClick = onRiprova, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Riprova")
                    }
                }
            }

            uiState.notifiche.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nessuna notifica",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Quando un tuo viaggio si conclude ti invitiamo a recensirlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.notifiche, key = { it.id }) { notifica ->
                        NotificaCard(
                            notifica = notifica,
                            onApriRecensione = { onApriRecensione(notifica) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificaCard(
    notifica: Notifica,
    onApriRecensione: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notifica.letta) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!notifica.letta) {
                    Surface(
                        shape = CircleShape,
                        color = TravelOrange,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(Modifier.size(8.dp))
                }

                Text(
                    text = notifica.titolo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notifica.letta) FontWeight.Normal else FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                dataLeggibile(notifica.data)?.let { quando ->
                    Text(
                        text = quando,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = notifica.messaggio,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // L'azione compare solo se la notifica porta con se' la prenotazione da recensire
            if (notifica.tipo == TipoNotifica.INVITO_RECENSIONE && notifica.prenotazioneId != null) {
                Button(
                    onClick = onApriRecensione,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Lascia una recensione")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Notifiche")
@Composable
private fun NotifichePreview() {
    MaterialTheme {
        NotificheScreen(
            uiState = NotificheUiState(
                notifiche = listOf(
                    Notifica(
                        id = 1,
                        tipo = TipoNotifica.INVITO_RECENSIONE,
                        titolo = "Com'è andata?",
                        messaggio = "Il tuo viaggio \"Tour della Sila\" si è concluso: lascia una recensione.",
                        letta = false,
                        data = "2026-08-30T09:00:00",
                        prenotazioneId = 7,
                        itinerarioId = 3,
                        titoloViaggio = "Tour della Sila"
                    )
                ),
                nonLette = 1
            ),
            onBack = {},
            onApriRecensione = {},
            onRiprova = {}
        )
    }
}
