package com.example.travelapp.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.data.remote.dto.DisponibilitaItinerarioResponseDto
import com.example.travelapp.data.remote.dto.SessioneAttivitaResponseDto
import com.example.travelapp.data.remote.dto.dataLeggibile
import com.example.travelapp.data.remote.dto.isPrenotabile
import com.example.travelapp.data.remote.dto.prenotazioniAperte
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.ListaPreferiti
import com.example.travelapp.domain.model.Recensione
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.components.CaroselloImmagini
import com.example.travelapp.ui.components.StelleValutazione
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.FavoriteRed
import com.example.travelapp.ui.theme.TravelBg
import com.example.travelapp.ui.theme.TravelBlue
import com.example.travelapp.ui.theme.TravelBorder
import com.example.travelapp.ui.theme.TravelChipBg
import com.example.travelapp.ui.theme.TravelOrange
import com.example.travelapp.ui.theme.TravelSurface
import com.example.travelapp.ui.theme.TravelTextDark
import com.example.travelapp.ui.theme.TravelTextMuted
import com.example.travelapp.ui.util.formattaData

@Composable
fun ItinerarioDetailScreen(
    itinerario: Itinerario,
    viewModel: DetailViewModel = viewModel(),
    onBack: () -> Unit,
    onPrenota: (DisponibilitaItinerarioResponseDto) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(itinerario.id) {
        viewModel.caricaDisponibilitaItinerario(itinerario.id)
        viewModel.caricaPreferiti(itinerario.id)
        viewModel.caricaRecensioni(itinerario.id)
    }

    if (uiState.selettorePreferitiAperto) {
        DialogoSceltaLista(
            liste = uiState.listePreferiti,
            listeConItinerario = uiState.listeConItinerario,
            inCaricamento = uiState.preferitiInCaricamento,
            operazioneInCorso = uiState.operazionePreferitiInCorso,
            messaggio = uiState.messaggioPreferiti,
            errore = uiState.errorePreferiti,
            onListaClick = { lista ->
                viewModel.cambiaAppartenenzaLista(
                    lista,
                    itinerario.id
                )
            },
            onCreaLista = { nome ->
                viewModel.creaListaConItinerario(
                    nome,
                    itinerario.id
                )
            },
            onDismiss = viewModel::chiudiSelettorePreferiti
        )
    }

    Scaffold(
        bottomBar = {
            Surface(
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "A partire da",
                            style = MaterialTheme.typography.labelMedium,
                            color = TravelTextMuted
                        )

                        Text(
                            text = "€${itinerario.prezzoBase ?: "---"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TravelTextDark
                        )
                    }

                    Button(
                        onClick = {
                            val disponibilitaSelezionata =
                                uiState.disponibilitaItinerario
                                    .firstOrNull {
                                        it.id == uiState.idSelezionato
                                    }

                            disponibilitaSelezionata?.let {
                                onPrenota(it)
                            }
                        },
                        enabled = uiState.disponibilitaItinerario
                            .firstOrNull {
                                it.id == uiState.idSelezionato
                            }
                            ?.isPrenotabile() == true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TravelOrange
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Prenota",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TravelBg)
                .verticalScroll(rememberScrollState())
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {

                CaroselloImmagini(
                    immagini = itinerario.immagini,
                    contentDescription = itinerario.titolo,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 16.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.92f
                                ),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TravelTextDark
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.apriSelettorePreferiti(
                                itinerario.id
                            )
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.92f
                                ),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector =
                                if (uiState.ePreferito) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                            contentDescription =
                                if (uiState.ePreferito) {
                                    "Gestisci le liste di preferiti"
                                } else {
                                    "Aggiungi ai preferiti"
                                },
                            tint =
                                if (uiState.ePreferito) {
                                    FavoriteRed
                                } else {
                                    TravelTextDark
                                }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp
                        )
                    )
                    .background(TravelSurface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TravelChipBg
                ) {
                    Text(
                        text = "ITINERARIO GUIDATO",
                        color = TravelBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    )
                }

                Text(
                    text = itinerario.titolo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    itinerario.destinazionePrincipale?.let { destinazione ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TravelTextMuted,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text(
                                text = destinazione,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TravelTextMuted
                            )
                        }
                    }

                    itinerario.durataGiorni?.let { durata ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = TravelTextMuted,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text(
                                text = "$durata giorni",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TravelTextMuted
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = TravelBorder
                )

                StelleValutazione(
                    media = itinerario.mediaVoti,
                    numeroRecensioni = itinerario.numeroRecensioni,
                    dimensione = 18.dp
                )

                HorizontalDivider(
                    color = TravelBorder
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date e posti disponibili",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TravelTextDark
                    )

                    if (!itinerario.dateDisponibili) {
                        EtichettaNessunaData()
                    }
                }

                if (uiState.isLoading) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = TravelBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                } else if (
                    uiState.disponibilitaItinerario.isEmpty()
                ) {

                    Text(
                        text = "Nessuna data attualmente disponibile per questo itinerario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TravelTextMuted
                    )

                } else {

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(
                            vertical = 4.dp
                        )
                    ) {

                        items(
                            uiState.disponibilitaItinerario
                        ) { disp ->

                            val isSelected =
                                uiState.idSelezionato == disp.id

                            val aperte =
                                disp.prenotazioniAperte()

                            val termine =
                                dataLeggibile(
                                    disp.dataLimitePrenotazione
                                )

                            SlotDateCard(
                                title =
                                    "${dataLeggibile(disp.dataInizio) ?: "-"} → " +
                                            "${dataLeggibile(disp.dataFine) ?: "-"}",
                                subtitle =
                                    if (disp.postiDisponibili > 0) {
                                        "${disp.postiDisponibili} posti disponibili"
                                    } else {
                                        "Nessun posto disponibile"
                                    },
                                nota =
                                    when {
                                        !aperte ->
                                            "Prenotazioni chiuse"

                                        disp.postiDisponibili <= 0 ->
                                            "Esaurito"

                                        termine != null ->
                                            "Prenota entro il $termine"

                                        else ->
                                            null
                                    },
                                enabled = disp.isPrenotabile(),
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.selezionaSlot(
                                        disp.id
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = TravelBorder
                )

                Text(
                    text = "Descrizione del viaggio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                Text(
                    text =
                        itinerario.descrizione
                            ?: "Nessuna descrizione disponibile per questo itinerario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TravelTextMuted,
                    lineHeight = 22.sp
                )

                HorizontalDivider(
                    color = TravelBorder
                )

                Text(
                    text = "Programma dell'itinerario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                if (itinerario.programma.isEmpty()) {

                    // Capita solo sugli itinerari pubblicati prima che il programma
                    // diventasse obbligatorio: meglio dirlo che mostrare il nulla.
                    Text(
                        text = "L'organizzatore non ha ancora pubblicato il programma di questo itinerario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TravelTextMuted
                    )

                } else {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        itinerario.programma.forEach { giornata ->

                            TappaItem(
                                giorno = "Giorno ${giornata.giorno}",
                                titolo = giornata.titolo,
                                desc = giornata.descrizione
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = TravelBorder
                )

                SezioneRecensioni(
                    recensioni = uiState.recensioni,
                    inCaricamento = uiState.recensioniInCaricamento
                )
            }
        }
    }
}

@Composable
fun AttivitaDetailScreen(
    attivita: SingolaAttivita,
    viewModel: DetailViewModel = viewModel(),
    onBack: () -> Unit,
    onPrenota: (SessioneAttivitaResponseDto) -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(attivita.id) {
        viewModel.caricaSessioniAttivita(
            attivita.id
        )
    }

    Scaffold(
        bottomBar = {

            Surface(
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            text = "Prezzo a persona",
                            style = MaterialTheme.typography.labelMedium,
                            color = TravelTextMuted
                        )

                        Text(
                            text = "€${attivita.prezzo ?: "---"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TravelTextDark
                        )
                    }

                    Button(
                        onClick = {

                            val sessioneSelezionata =
                                uiState.sessioniAttivita
                                    .firstOrNull {
                                        it.id == uiState.idSelezionato
                                    }

                            sessioneSelezionata?.let {
                                onPrenota(it)
                            }
                        },
                        enabled = uiState.idSelezionato != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TravelOrange
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {

                        Text(
                            text = "Prenota",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TravelBg)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {

                CaroselloImmagini(
                    immagini = attivita.immagini,
                    contentDescription = attivita.titolo,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 16.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.92f
                                ),
                                CircleShape
                            )
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TravelTextDark
                        )
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.92f
                                ),
                                CircleShape
                            )
                    ) {

                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Preferiti",
                            tint = TravelTextDark
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp
                        )
                    )
                    .background(TravelSurface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TravelChipBg
                ) {

                    Text(
                        text = "ATTIVITÀ ESPERIENZIALE",
                        color = TravelBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    )
                }

                Text(
                    text = attivita.titolo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                attivita.luogo?.let { luogo ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TravelTextMuted,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        Text(
                            text = luogo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TravelTextMuted
                        )
                    }
                }

                HorizontalDivider(
                    color = TravelBorder
                )

                Text(
                    text = "Sessioni disponibili",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                if (uiState.isLoading) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = TravelBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                } else if (
                    uiState.sessioniAttivita.isEmpty()
                ) {

                    Text(
                        text = "Nessuna sessione programmata per questa attività.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TravelTextMuted
                    )

                } else {

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(
                            vertical = 4.dp
                        )
                    ) {

                        items(
                            uiState.sessioniAttivita
                        ) { sess ->

                            val isSelected =
                                uiState.idSelezionato ==
                                        sess.id

                            SlotDateCard(
                                title =
                                    formattaData(
                                        sess.dataInizio
                                    ),
                                subtitle =
                                    "${sess.postiDisponibili} posti",
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.selezionaSlot(
                                        sess.id
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = TravelBorder
                )

                Text(
                    text = "Descrizione dell'esperienza",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelTextDark
                )

                Text(
                    text =
                        attivita.descrizione
                            ?: "Nessuna descrizione disponibile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TravelTextMuted,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun SlotDateCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    nota: String? = null,
    enabled: Boolean = true
) {

    Surface(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .border(
                width =
                    if (isSelected) {
                        2.dp
                    } else {
                        1.dp
                    },
                color =
                    if (isSelected) {
                        TravelBlue
                    } else {
                        TravelBorder
                    },
                shape =
                    RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color =
            if (isSelected) {
                TravelChipBg
            } else {
                TravelSurface
            }
    ) {

        Column(
            modifier =
                Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color =
                    when {
                        !enabled ->
                            TravelTextMuted

                        isSelected ->
                            TravelBlue

                        else ->
                            TravelTextDark
                    }
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TravelTextMuted
            )

            if (nota != null) {

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = nota,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (enabled) {
                            TravelBlue
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                )
            }
        }
    }
}

@Composable
private fun TappaItem(
    giorno: String,
    titolo: String,
    desc: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 6.dp)
                .background(
                    TravelBlue,
                    CircleShape
                )
        )

        Column {

            Text(
                text = "$giorno: $titolo",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TravelTextDark
            )

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TravelTextMuted
            )
        }
    }
}

@Composable
private fun SezioneRecensioni(
    recensioni: List<Recensione>,
    inCaricamento: Boolean
) {

    Text(
        text = "Recensioni dei viaggiatori",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TravelTextDark
    )

    when {

        inCaricamento -> {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = TravelBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        recensioni.isEmpty() -> {

            Text(
                text = "Ancora nessuna recensione per questo itinerario.",
                style = MaterialTheme.typography.bodySmall,
                color = TravelTextMuted
            )
        }

        else -> {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                recensioni.forEach { recensione ->
                    RecensioneItem(
                        recensione
                    )
                }
            }
        }
    }
}

@Composable
private fun RecensioneItem(
    recensione: Recensione
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                TravelBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = recensione.autore,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TravelTextDark
            )

            dataLeggibile(
                recensione.data
            )?.let { quando ->

                Text(
                    text = quando,
                    style = MaterialTheme.typography.bodySmall,
                    color = TravelTextMuted
                )
            }
        }

        StelleValutazione(
            media =
                recensione.votazione.toDouble(),
            numeroRecensioni = 1,
            mostraConteggio = false
        )

        recensione.commento
            ?.let { commento ->

                Text(
                    text = commento,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TravelTextMuted
                )
            }
    }
}

@Composable
private fun DialogoSceltaLista(
    liste: List<ListaPreferiti>,
    listeConItinerario: Set<Long>,
    inCaricamento: Boolean,
    operazioneInCorso: Boolean,
    messaggio: String?,
    errore: String?,
    onListaClick: (ListaPreferiti) -> Unit,
    onCreaLista: (String) -> Unit,
    onDismiss: () -> Unit
) {

    var creazioneAperta by remember {
        mutableStateOf(false)
    }

    var nuovoNome by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TravelSurface,
        title = {

            Text(
                text = "Salva nei preferiti",
                fontWeight = FontWeight.Bold,
                color = TravelTextDark
            )
        },
        text = {

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text =
                        "In quale lista vuoi metterlo?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TravelTextMuted
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                when {

                    inCaricamento -> {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 20.dp
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            CircularProgressIndicator(
                                color = TravelBlue,
                                modifier =
                                    Modifier.size(28.dp)
                            )
                        }
                    }

                    liste.isEmpty() -> {

                        Text(
                            text =
                                "Non hai ancora nessuna lista: creane una qui sotto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TravelTextMuted
                        )
                    }

                    else -> {

                        Column(
                            modifier = Modifier
                                .heightIn(
                                    max = 260.dp
                                )
                                .verticalScroll(
                                    rememberScrollState()
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            liste.forEach { lista ->

                                RigaLista(
                                    lista = lista,
                                    selezionata =
                                        lista.id in
                                                listeConItinerario,
                                    abilitata =
                                        !operazioneInCorso,
                                    onClick = {
                                        onListaClick(
                                            lista
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                HorizontalDivider(
                    color = TravelBorder
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                if (creazioneAperta) {

                    OutlinedTextField(
                        value = nuovoNome,
                        onValueChange = {
                            nuovoNome = it
                        },
                        singleLine = true,
                        label = {
                            Text(
                                "Nome della nuova lista"
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = {
                                onCreaLista(
                                    nuovoNome
                                )
                                nuovoNome = ""
                                creazioneAperta =
                                    false
                            },
                            enabled =
                                nuovoNome.isNotBlank() &&
                                        !operazioneInCorso,
                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            TravelBlue
                                    ),
                            shape =
                                RoundedCornerShape(
                                    10.dp
                                )
                        ) {

                            Text(
                                text = "Crea e salva",
                                color = Color.White
                            )
                        }

                        TextButton(
                            onClick = {
                                creazioneAperta =
                                    false
                                nuovoNome = ""
                            }
                        ) {

                            Text(
                                text = "Annulla",
                                color =
                                    TravelTextMuted
                            )
                        }
                    }

                } else {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    10.dp
                                )
                            )
                            .clickable(
                                enabled =
                                    !operazioneInCorso
                            ) {
                                creazioneAperta =
                                    true
                            }
                            .padding(
                                vertical = 8.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Add,
                            contentDescription = null,
                            tint = TravelBlue,
                            modifier =
                                Modifier.size(20.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "Crea una nuova lista",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                            fontWeight =
                                FontWeight.Medium,
                            color = TravelBlue
                        )
                    }
                }

                if (operazioneInCorso) {

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    LinearProgressIndicator(
                        color = TravelBlue,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                val avviso =
                    errore ?: messaggio

                avviso?.let { testo ->

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text = testo,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            if (errore != null) {
                                ErrorRed
                            } else {
                                TravelBlue
                            }
                    )
                }
            }
        },
        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "Fine",
                    color = TravelBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun RigaLista(
    lista: ListaPreferiti,
    selezionata: Boolean,
    abilitata: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                if (selezionata) {
                    TravelChipBg
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                enabled = abilitata,
                onClick = onClick
            )
            .padding(
                horizontal = 10.dp,
                vertical = 10.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(
                    RoundedCornerShape(6.dp)
                )
                .background(
                    if (selezionata) {
                        TravelBlue
                    } else {
                        Color.Transparent
                    }
                )
                .border(
                    width = 1.5.dp,
                    color =
                        if (selezionata) {
                            TravelBlue
                        } else {
                            TravelBorder
                        },
                    shape =
                        RoundedCornerShape(
                            6.dp
                        )
                ),
            contentAlignment =
                Alignment.Center
        ) {

            if (selezionata) {

                Icon(
                    imageVector =
                        Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier =
                        Modifier.size(16.dp)
                )
            }
        }

        Spacer(
            modifier =
                Modifier.width(12.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = lista.nome,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                fontWeight =
                    FontWeight.Medium,
                color = TravelTextDark
            )

            Text(
                text =
                    if (
                        lista.numeroItinerari == 1
                    ) {
                        "1 itinerario"
                    } else {
                        "${lista.numeroItinerari} itinerari"
                    },
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color = TravelTextMuted
            )
        }

        Icon(
            imageVector =
                if (lista.eCondivisa) {
                    Icons.Default.Share
                } else {
                    Icons.Default.Lock
                },
            contentDescription =
                if (lista.eCondivisa) {
                    "Lista condivisa"
                } else {
                    "Lista privata"
                },
            tint = TravelTextMuted,
            modifier =
                Modifier.size(16.dp)
        )
    }
}