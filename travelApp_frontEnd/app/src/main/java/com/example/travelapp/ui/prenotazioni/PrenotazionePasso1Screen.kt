package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelapp.ui.theme.AccentOrange
import com.example.travelapp.ui.theme.DividerColor
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.util.formattaData
data class ExtraUi(
    val id: Long,
    val nome: String,
    val prezzo: Double
)

@Composable
fun PrenotazionePasso1Screen(
    uiState: BookingUiState,
    extraDisponibili: List<ExtraUi>,
    onIncrementa: () -> Unit,
    onDecrementa: () -> Unit,
    onToggleExtra: (Long, Double) -> Unit,
    onContinua: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {
        Text(
            text = "Completa la prenotazione",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(
            modifier = Modifier.height(6.dp)
        )
        Text(
            text = "Scegli i partecipanti e personalizza la tua esperienza.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(
            modifier = Modifier.height(20.dp)
        )
        BookingSteps(
            currentStep = 1
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (
            uiState.titolo.isNotBlank() ||
            uiState.luogo.isNotBlank()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceWhite
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    if (uiState.titolo.isNotBlank()) {
                        Text(
                            text = uiState.titolo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (uiState.luogo.isNotBlank()) {

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = uiState.luogo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    uiState.dataInizio?.let { data ->

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                if (uiState.dataFine != null) {
                                    "${formattaData(data)} - ${formattaData(uiState.dataFine)}"
                                } else {
                                    formattaData(data)
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }

        Text(
            text = "Partecipanti",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceWhite
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Numero di persone",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Text(
                        text = "Seleziona i partecipanti",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    uiState.postiDisponibili?.let { posti ->

                        Text(
                            text = "$posti posti disponibili",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        IconButton(
                            onClick = onDecrementa,
                            enabled = uiState.numeroPartecipanti > 1,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = uiState.numeroPartecipanti.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        IconButton(
                            onClick = onIncrementa,
                            enabled = uiState.puoIncrementarePartecipanti,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Aumenta partecipanti",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (extraDisponibili.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Extra consigliati",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Aggiungi esperienze opzionali alla prenotazione.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                extraDisponibili.forEach { extra ->

                    val selezionato =
                        extra.id in uiState.extraSelezionati

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceWhite
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 1.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleExtra(
                                    extra.id,
                                    extra.prezzo
                                )
                            }
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Checkbox(
                                checked = selezionato,
                                onCheckedChange = {
                                    onToggleExtra(
                                        extra.id,
                                        extra.prezzo
                                    )
                                }
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            ) {

                                Text(
                                    text = extra.nome,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )

                                Text(
                                    text = "€${"%.2f".format(extra.prezzo)} a persona",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Text(
                                text = "+ €${"%.2f".format(extra.prezzo)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        Spacer(
            modifier = Modifier.height(24.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceWhite
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Riepilogo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                RigaPrezzo(
                    nome = "Partecipanti (${uiState.numeroPartecipanti})",
                    valore = uiState.prezzoBase
                )
                RigaPrezzo(
                    nome = "Extra",
                    valore = uiState.prezzoExtra
                )
                HorizontalDivider(
                    color = DividerColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Totale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "€${"%.2f".format(uiState.prezzoTotaleVisualizzato)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Il prezzo definitivo sarà verificato dal server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(
            modifier = Modifier.height(20.dp)
        )
        Button(
            onClick = onContinua,
            enabled = !uiState.isLoading && !uiState.extraInCaricamento,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = when {
                    uiState.extraInCaricamento ->
                        "Caricamento extra..."

                    uiState.isLoading ->
                        "Elaborazione..."
                    else ->
                        "Continua al pagamento"
                },
                fontWeight = FontWeight.Bold
            )
        }
        uiState.errore?.let { errore ->
            Spacer(
                modifier = Modifier.height(10.dp)
            )
            Text(
                text = errore,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun RigaPrezzo(
    nome: String,
    valore: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = nome,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = "€${"%.2f".format(valore)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun BookingSteps(
    currentStep: Int
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            StepCircle(
                numero = 1,
                titolo = "Prenotazione",
                attivo = currentStep >= 1,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .height(1.dp)
            ) {
                HorizontalDivider()
            }

            StepCircle(
                numero = 2,
                titolo = "Pagamento",
                attivo = currentStep >= 2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StepCircle(
    numero: Int,
    titolo: String,
    attivo: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Surface(
            shape = CircleShape,
            color = if (attivo) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {

            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = numero.toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (attivo) {
                        Color.White
                    } else {
                        TextSecondary
                    }
                )
            }
        }
        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = titolo,
            style = MaterialTheme.typography.labelSmall,
            color = if (attivo) {
                TextPrimary
            } else {
                TextSecondary
            }
        )
    }
}