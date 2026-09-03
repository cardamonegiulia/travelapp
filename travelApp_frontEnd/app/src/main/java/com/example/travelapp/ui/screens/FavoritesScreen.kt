package com.example.travelapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.ListaPreferiti
import com.example.travelapp.domain.model.VisibilitaLista
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.components.FavoriteTrip
import com.example.travelapp.ui.components.FavoriteTripCard
import com.example.travelapp.ui.preferiti.PreferitiUiState
import com.example.travelapp.ui.preferiti.SezionePreferiti
import com.example.travelapp.ui.theme.AccentOrange
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.BadgeBlue
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.FieldBorder
import com.example.travelapp.ui.theme.LogoutBackground
import com.example.travelapp.ui.theme.OutlineGrey
import com.example.travelapp.ui.theme.PrimaryBlue
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary

@Composable
fun FavoritesScreen(
    state: PreferitiUiState,
    onBack: () -> Unit,
    onSectionChange: (SezionePreferiti) -> Unit,
    onOpenList: (Long) -> Unit,
    onCloseList: () -> Unit,
    onCreateList: (String, VisibilitaLista) -> Unit,
    onChangeVisibility: (ListaPreferiti, VisibilitaLista) -> Unit,
    onDeleteList: (Long) -> Unit,
    onRemoveTrip: (Long, Long) -> Unit,
    onShareWithEmail: (Long, String) -> Unit,
    onRevokeShare: (Long, Long) -> Unit,
    onTripClick: (Long) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listaAperta = state.listaAperta

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = listaAperta?.nome ?: "Preferiti",
                onBack = if (listaAperta != null) onCloseList else onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Avvisi(
                messaggio = state.messaggio,
                errore = state.errore,
                onMessageShown = onMessageShown
            )

            if (state.isLoading) {
                Caricamento()
            } else if (listaAperta != null) {
                DettaglioLista(
                    lista = listaAperta,
                    onChangeVisibility = onChangeVisibility,
                    onDeleteList = onDeleteList,
                    onRemoveTrip = onRemoveTrip,
                    onShareWithEmail = onShareWithEmail,
                    onRevokeShare = onRevokeShare,
                    onTripClick = onTripClick
                )
            } else {
                ElencoListe(
                    state = state,
                    onSectionChange = onSectionChange,
                    onOpenList = onOpenList,
                    onCreateList = onCreateList
                )
            }
        }
    }
}


@Composable
private fun ElencoListe(
    state: PreferitiUiState,
    onSectionChange: (SezionePreferiti) -> Unit,
    onOpenList: (Long) -> Unit,
    onCreateList: (String, VisibilitaLista) -> Unit
) {
    var creazioneAperta by remember { mutableStateOf(false) }

    if (creazioneAperta) {
        DialogoNuovaLista(
            onDismiss = { creazioneAperta = false },
            onConfirm = { nome, visibilita ->
                creazioneAperta = false
                onCreateList(nome, visibilita)
            }
        )
    }

    SelettoreSezione(
        sezione = state.sezione,
        onSectionChange = onSectionChange,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )

    val liste = state.listeVisibili

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.sezione == SezionePreferiti.MIE) {
            item {
                OutlinedButton(
                    onClick = { creazioneAperta = true },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, OutlineGrey),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceWhite,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Nuova lista", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (liste.isEmpty()) {
            item {
                Text(
                    text = when (state.sezione) {
                        SezionePreferiti.MIE ->
                            "Non hai ancora creato nessuna lista. Creane una per raccogliere i viaggi che ti interessano."
                        SezionePreferiti.CONDIVISE_CON_ME ->
                            "Nessuno ha ancora condiviso una lista con te."
                    },
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }

        items(items = liste, key = { lista -> lista.id }) { lista ->
            CardLista(lista = lista, onClick = { onOpenList(lista.id) })
        }
    }
}


@Composable
private fun CardLista(
    lista: ListaPreferiti,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lista.nome,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BadgeVisibilita(lista = lista)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = sottotitoloLista(lista),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}


private fun sottotitoloLista(lista: ListaPreferiti): String {
    val itinerari = when (lista.numeroItinerari) {
        0 -> "nessun itinerario"
        1 -> "1 itinerario"
        else -> "${lista.numeroItinerari} itinerari"
    }

    return if (!lista.proprietaria) {
        val proprietario = lista.proprietarioNome?.takeIf { it.isNotBlank() } ?: "un altro viaggiatore"
        "$itinerari · condivisa da $proprietario"
    } else if (lista.eCondivisa) {
        val persone = when (lista.destinatari.size) {
            0 -> "non ancora condivisa con nessuno"
            1 -> "condivisa con 1 persona"
            else -> "condivisa con ${lista.destinatari.size} persone"
        }
        "$itinerari · $persone"
    } else {
        "$itinerari · visibile solo a te"
    }
}

@Composable
private fun BadgeVisibilita(lista: ListaPreferiti, modifier: Modifier = Modifier) {
    val condivisa = lista.eCondivisa
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = if (condivisa) BadgeBlue else Color(0xFFEDEEF0),
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = if (condivisa) Icons.Default.Share else Icons.Default.Lock,
            contentDescription = null,
            tint = if (condivisa) PrimaryBlue else TextSecondary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = if (condivisa) "Condivisa" else "Privata",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (condivisa) PrimaryBlue else TextSecondary
        )
    }
}


@Composable
private fun DettaglioLista(
    lista: ListaPreferiti,
    onChangeVisibility: (ListaPreferiti, VisibilitaLista) -> Unit,
    onDeleteList: (Long) -> Unit,
    onRemoveTrip: (Long, Long) -> Unit,
    onShareWithEmail: (Long, String) -> Unit,
    onRevokeShare: (Long, Long) -> Unit,
    onTripClick: (Long) -> Unit
) {
    var condivisioneAperta by remember { mutableStateOf(false) }
    var confermaEliminazione by remember { mutableStateOf(false) }

    if (condivisioneAperta) {
        DialogoCondivisione(
            onDismiss = { condivisioneAperta = false },
            onConfirm = { email ->
                condivisioneAperta = false
                onShareWithEmail(lista.id, email)
            }
        )
    }

    if (confermaEliminazione) {
        AlertDialog(
            onDismissRequest = { confermaEliminazione = false },
            title = { Text(text = "Eliminare la lista?") },
            text = {
                Text(
                    text = "Sparisce solo la lista \"${lista.nome}\": gli itinerari restano " +
                            "nel catalogo e nelle altre tue liste."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confermaEliminazione = false
                    onDeleteList(lista.id)
                }) {
                    Text(text = "Elimina", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confermaEliminazione = false }) {
                    Text(text = "Annulla")
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            IntestazioneDettaglio(
                lista = lista,
                onChangeVisibility = onChangeVisibility,
                onApriCondivisione = { condivisioneAperta = true },
                onEliminaLista = { confermaEliminazione = true }
            )
        }

        if (lista.proprietaria && lista.eCondivisa && lista.destinatari.isNotEmpty()) {
            item {
                Text(
                    text = "Ha accesso a questa lista",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
            items(items = lista.destinatari, key = { destinatario -> "utente-${destinatario.id}" }) { destinatario ->
                RigaDestinatario(
                    nome = destinatario.nomeCompleto.ifBlank { destinatario.email },
                    email = destinatario.email,
                    onRevoke = { onRevokeShare(lista.id, destinatario.id) }
                )
            }
        }

        if (lista.itinerari.isEmpty()) {
            item {
                Text(
                    text = "Questa lista è ancora vuota.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        items(items = lista.itinerari, key = { itinerario -> itinerario.id }) { itinerario ->
            FavoriteTripCard(
                trip = itinerario.toFavoriteTrip(),
                onToggleFavorite = { itinerarioId -> onRemoveTrip(lista.id, itinerarioId) },
                onClick = onTripClick,
                showFavoriteBadge = lista.proprietaria
            )
        }
    }
}

@Composable
private fun IntestazioneDettaglio(
    lista: ListaPreferiti,
    onChangeVisibility: (ListaPreferiti, VisibilitaLista) -> Unit,
    onApriCondivisione: () -> Unit,
    onEliminaLista: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgeVisibilita(lista = lista)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sottotitoloLista(lista),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        if (!lista.proprietaria) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lista condivisa con te: puoi consultarla, non modificarla.",
                fontSize = 13.sp,
                color = TextSecondary
            )
            return
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lista.eCondivisa) {
                Button(
                    onClick = onApriCondivisione,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Condividi", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { onChangeVisibility(lista, VisibilitaLista.PRIVATA) },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, OutlineGrey),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceWhite,
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = "Rendi privata", fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick = { onChangeVisibility(lista, VisibilitaLista.CONDIVISA) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Rendi condivisibile", fontSize = 13.sp)
                }
            }

            IconButton(onClick = onEliminaLista) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Elimina la lista",
                    tint = ErrorRed
                )
            }
        }
    }
}

@Composable
private fun RigaDestinatario(
    nome: String,
    email: String,
    onRevoke: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(color = SurfaceWhite, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nome,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (email.isNotBlank() && email != nome) {
                Text(
                    text = email,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        TextButton(onClick = onRevoke) {
            Text(text = "Revoca", fontSize = 13.sp, color = ErrorRed)
        }
    }
}


@Composable
private fun DialogoNuovaLista(
    onDismiss: () -> Unit,
    onConfirm: (String, VisibilitaLista) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var visibilita by remember { mutableStateOf(VisibilitaLista.PRIVATA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nuova lista") },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    singleLine = true,
                    label = { Text(text = "Nome della lista") },
                    colors = campoColori(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Chi può vederla",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillSelezionabile(
                        testo = "Solo io",
                        selezionato = visibilita == VisibilitaLista.PRIVATA,
                        onClick = { visibilita = VisibilitaLista.PRIVATA }
                    )
                    PillSelezionabile(
                        testo = "Utenti scelti da me",
                        selezionato = visibilita == VisibilitaLista.CONDIVISA,
                        onClick = { visibilita = VisibilitaLista.CONDIVISA }
                    )
                }
                if (visibilita == VisibilitaLista.CONDIVISA) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dopo averla creata scegli tu, una per una, le persone che potranno aprirla.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nome, visibilita) },
                enabled = nome.isNotBlank()
            ) {
                Text(text = "Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Annulla") }
        }
    )
}

@Composable
private fun DialogoCondivisione(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Condividi la lista") },
        text = {
            Column {
                Text(
                    text = "L'email dell'utente che potrà consultare la lista. Vedrà gli itinerari, " +
                            "ma non potrà modificarli.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    label = { Text(text = "Email") },
                    colors = campoColori(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(email) },
                enabled = email.isNotBlank()
            ) {
                Text(text = "Condividi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Annulla") }
        }
    )
}

@Composable
private fun campoColori() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = FieldBorder,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = PrimaryBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)


@Composable
private fun SelettoreSezione(
    sezione: SezionePreferiti,
    onSectionChange: (SezionePreferiti) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        SezionePreferiti.entries.forEach { voce ->
            PillSelezionabile(
                testo = voce.etichetta,
                selezionato = voce == sezione,
                onClick = { onSectionChange(voce) }
            )
        }
    }
}

@Composable
private fun PillSelezionabile(
    testo: String,
    selezionato: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = if (selezionato) PrimaryBlue else SurfaceWhite,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = testo,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selezionato) Color.White else TextPrimary
        )
    }
}

@Composable
private fun Avvisi(
    messaggio: String?,
    errore: String?,
    onMessageShown: () -> Unit
) {
    val testo = errore ?: messaggio ?: return
    val sfondo = if (errore != null) LogoutBackground else BadgeBlue
    val colore = if (errore != null) ErrorRed else PrimaryBlue

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(color = sfondo, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = testo,
            fontSize = 13.sp,
            color = colore,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onMessageShown) {
            Text(text = "OK", fontSize = 13.sp, color = colore)
        }
    }
}

@Composable
private fun Caricamento() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        CircularProgressIndicator(color = AccentOrange)
    }
}

/** Adatta un itinerario del backend alla card gia' esistente dei preferiti. */
private fun Itinerario.toFavoriteTrip(): FavoriteTrip = FavoriteTrip(
    id = id,
    title = titolo,
    imageUrl = immagini.firstOrNull()?.url,
    days = durataGiorni ?: 0,
    priceFrom = prezzoBase?.toInt() ?: 0
)


/** Dati segnaposto per le anteprime: a runtime lo stato arriva dal PreferitiViewModel. */
internal val sampleListePreferiti = listOf(
    ListaPreferiti(
        id = 1,
        nome = "I miei preferiti",
        visibilita = VisibilitaLista.PRIVATA,
        proprietarioId = 1,
        proprietarioNome = "Marco Rossi",
        proprietaria = true,
        numeroItinerari = 3
    ),
    ListaPreferiti(
        id = 2,
        nome = "Estate con gli amici",
        visibilita = VisibilitaLista.CONDIVISA,
        proprietarioId = 1,
        proprietarioNome = "Marco Rossi",
        proprietaria = true,
        numeroItinerari = 5
    )
)

@Preview(showBackground = true, showSystemUi = true, name = "Preferiti - liste")
@Composable
private fun FavoritesScreenPreview() {
    MaterialTheme {
        FavoritesScreen(
            state = PreferitiUiState(isLoading = false, mieListe = sampleListePreferiti),
            onBack = {},
            onSectionChange = {},
            onOpenList = {},
            onCloseList = {},
            onCreateList = { _, _ -> },
            onChangeVisibility = { _, _ -> },
            onDeleteList = {},
            onRemoveTrip = { _, _ -> },
            onShareWithEmail = { _, _ -> },
            onRevokeShare = { _, _ -> },
            onTripClick = {},
            onMessageShown = {}
        )
    }
}
