package com.example.travelapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.ui.prenotazioni.PrenotazioneCard
import com.example.travelapp.ui.prenotazioni.SchedaPrenotazioni
import com.example.travelapp.ui.prenotazioni.ViaggioConclusoCard
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary

/**
 * Sezione prenotazioni, divisa in due schede.
 *
 * "Prenotazioni" resta quello che era; "Viaggi conclusi" e' la lista da cui si lascia una
 * recensione. La divisione arriva dal server: qui non si guardano le date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    prenotazioni: List<Prenotazione> = emptyList(),
    viaggiConclusi: List<Prenotazione> = emptyList(),
    schedaSelezionata: SchedaPrenotazioni = SchedaPrenotazioni.ATTUALI,
    notificheNonLette: Long = 0,
    isLoading: Boolean = false,
    errore: String? = null,
    onSchedaSelezionata: (SchedaPrenotazioni) -> Unit = {},
    onRiprova: () -> Unit = {},
    onPrenotazioneClick: (Prenotazione) -> Unit = {},
    onRecensisci: (Prenotazione) -> Unit = {},
    onNotificheClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "I miei viaggi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    CampanelloNotifiche(
                        nonLette = notificheNonLette,
                        onClick = onNotificheClick
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            TabRow(
                selectedTabIndex = schedaSelezionata.ordinal,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = schedaSelezionata == SchedaPrenotazioni.ATTUALI,
                    onClick = { onSchedaSelezionata(SchedaPrenotazioni.ATTUALI) },
                    text = { Text("Prenotazioni") }
                )
                Tab(
                    selected = schedaSelezionata == SchedaPrenotazioni.CONCLUSI,
                    onClick = { onSchedaSelezionata(SchedaPrenotazioni.CONCLUSI) },
                    text = { Text("Viaggi conclusi") }
                )
            }

            when {

                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errore != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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

                schedaSelezionata == SchedaPrenotazioni.ATTUALI -> {
                    ElencoPrenotazioni(
                        prenotazioni = prenotazioni,
                        titoloVuoto = "Nessuna prenotazione",
                        testoVuoto = "Le prenotazioni che effettuerai compariranno qui.",
                        onPrenotazioneClick = onPrenotazioneClick
                    )
                }

                else -> {
                    ElencoViaggiConclusi(
                        viaggi = viaggiConclusi,
                        onPrenotazioneClick = onPrenotazioneClick,
                        onRecensisci = onRecensisci
                    )
                }
            }
        }
    }
}

@Composable
private fun CampanelloNotifiche(
    nonLette: Long,
    onClick: () -> Unit
) {
    Box {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = if (nonLette > 0) {
                    "Notifiche, $nonLette da leggere"
                } else {
                    "Notifiche"
                },
                tint = TextPrimary
            )
        }

        if (nonLette > 0) {
            Surface(
                shape = CircleShape,
                color = ErrorRed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(10.dp)
            ) {}
        }
    }
}

@Composable
private fun ElencoPrenotazioni(
    prenotazioni: List<Prenotazione>,
    titoloVuoto: String,
    testoVuoto: String,
    onPrenotazioneClick: (Prenotazione) -> Unit
) {
    if (prenotazioni.isEmpty()) {
        StatoVuoto(titolo = titoloVuoto, testo = testoVuoto)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = prenotazioni,
            key = { prenotazione -> prenotazione.id }
        ) { prenotazione ->
            PrenotazioneCard(
                prenotazione = prenotazione,
                onClick = { onPrenotazioneClick(prenotazione) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ElencoViaggiConclusi(
    viaggi: List<Prenotazione>,
    onPrenotazioneClick: (Prenotazione) -> Unit,
    onRecensisci: (Prenotazione) -> Unit
) {
    if (viaggi.isEmpty()) {
        StatoVuoto(
            titolo = "Nessun viaggio concluso",
            testo = "Qui troverai i viaggi che hai già fatto, pronti da recensire."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = viaggi,
            key = { viaggio -> viaggio.id }
        ) { viaggio ->
            ViaggioConclusoCard(
                prenotazione = viaggio,
                onClick = { onPrenotazioneClick(viaggio) },
                onRecensisci = { onRecensisci(viaggio) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatoVuoto(
    titolo: String,
    testo: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = titolo,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            // textAlign, non solo l'allineamento della Column: quello centra il blocco di
            // testo, ma su piu' righe le righe stesse resterebbero allineate a sinistra
            Text(
                text = testo,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
