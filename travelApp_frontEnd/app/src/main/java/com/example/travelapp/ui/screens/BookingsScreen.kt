package com.example.travelapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.prenotazioni.PrenotazioneCard
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary

@Composable
fun BookingsScreen(
    prenotazioni: List<Prenotazione> = emptyList(),
    isLoading: Boolean = false,
    errore: String? = null,
    onRiprova: () -> Unit = {},
    onPrenotazioneClick: (Prenotazione) -> Unit = {},
    modifier: Modifier = Modifier
) {

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "Le mie prenotazioni"
            )
        }
    ) { innerPadding ->

        when {

            // 1. CARICAMENTO
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

            // 2. ERRORE
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
                        text = "Impossibile caricare le prenotazioni",
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

            // 3. LISTA VUOTA
            prenotazioni.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Nessuna prenotazione",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Text(
                            text = "Le prenotazioni che effettuerai compariranno qui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // 4. PRENOTAZIONI PRESENTI
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
                        items = prenotazioni,
                        key = { prenotazione -> prenotazione.id }
                    ) { prenotazione ->

                        PrenotazioneCard(
                            prenotazione = prenotazione,
                            onClick = {
                                onPrenotazioneClick(prenotazione)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}