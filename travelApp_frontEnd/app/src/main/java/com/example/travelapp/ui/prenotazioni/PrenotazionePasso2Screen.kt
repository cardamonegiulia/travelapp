package com.example.travelapp.ui.prenotazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.travelapp.ui.theme.AccentOrange
import com.example.travelapp.ui.theme.DividerColor
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import com.example.travelapp.ui.theme.WarningBackground
import com.example.travelapp.ui.theme.WarningYellow
import com.example.travelapp.ui.util.formattaData

@Composable
fun PrenotazionePasso2Screen(
    uiState: BookingUiState,
    onMetodoPagamentoSelezionato: (MetodoPagamentoUi) -> Unit,
    onConfermaEPaga: () -> Unit
) {

    var numeroCarta by remember { mutableStateOf("") }
    var intestatario by remember { mutableStateOf("") }
    var scadenza by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    val numeroCartaValido =
        numeroCarta.filter { it.isDigit() }.length == 16

    val scadenzaValida =
        scadenza.matches(
            Regex("""\d{2}/\d{2}""")
        ) &&
                scadenza.take(2).toIntOrNull() in 1..12

    val datiCartaValidi =
        numeroCartaValido &&
                intestatario.isNotBlank() &&
                scadenzaValida &&
                cvv.isNotBlank()

    val pagamentoConsentito =
        when (uiState.metodoPagamento) {
            MetodoPagamentoUi.CARTA_CREDITO ->
                datiCartaValidi

            MetodoPagamentoUi.PAYPAL ->
                true

            MetodoPagamentoUi.BONIFICO ->
                true
        }

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
            text = "Pagamento",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Controlla il riepilogo e scegli come completare la prenotazione.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        PagamentoSteps()

        Spacer(
            modifier = Modifier.height(20.dp)
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
                    text = "Riepilogo prenotazione",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (uiState.titolo.isNotBlank()) {
                    Text(
                        text = uiState.titolo,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                if (uiState.luogo.isNotBlank()) {
                    Text(
                        text = uiState.luogo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                uiState.dataInizio?.let { data ->

                    RigaRiepilogoPagamento(
                        etichetta = "Data",
                        valore =
                            if (uiState.dataFine != null) {
                                "${formattaData(data)} - ${formattaData(uiState.dataFine)}"
                            } else {
                                formattaData(data)
                            }
                    )
                }

                HorizontalDivider(
                    color = DividerColor
                )

                RigaRiepilogoPagamento(
                    etichetta = "Partecipanti",
                    valore = uiState.numeroPartecipanti.toString()
                )

                RigaRiepilogoPagamento(
                    etichetta = "Subtotale",
                    valore = "€${"%.2f".format(uiState.prezzoBase)}"
                )

                RigaRiepilogoPagamento(
                    etichetta = "Extra",
                    valore = "€${"%.2f".format(uiState.prezzoExtra)}"
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
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Metodo di pagamento",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        MetodoPagamentoCard(
            titolo = "Carta di credito",
            descrizione = "Visa, Mastercard o carta di debito",
            selezionato =
                uiState.metodoPagamento == MetodoPagamentoUi.CARTA_CREDITO,
            onClick = {
                onMetodoPagamentoSelezionato(
                    MetodoPagamentoUi.CARTA_CREDITO
                )
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        MetodoPagamentoCard(
            titolo = "PayPal",
            descrizione = "Pagamento tramite account PayPal",
            selezionato =
                uiState.metodoPagamento == MetodoPagamentoUi.PAYPAL,
            onClick = {
                onMetodoPagamentoSelezionato(
                    MetodoPagamentoUi.PAYPAL
                )
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        MetodoPagamentoCard(
            titolo = "Bonifico bancario",
            descrizione = "Pagamento tramite coordinate bancarie",
            selezionato =
                uiState.metodoPagamento == MetodoPagamentoUi.BONIFICO,
            onClick = {
                onMetodoPagamentoSelezionato(
                    MetodoPagamentoUi.BONIFICO
                )
            }
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        when (uiState.metodoPagamento) {

            MetodoPagamentoUi.CARTA_CREDITO -> {

                CartaPagamentoForm(
                    numeroCarta = numeroCarta,
                    onNumeroCartaChange = {
                        numeroCarta = it
                    },
                    intestatario = intestatario,
                    onIntestatarioChange = {
                        intestatario = it
                    },
                    scadenza = scadenza,
                    onScadenzaChange = {
                        scadenza = it
                    },
                    cvv = cvv,
                    onCvvChange = {
                        cvv = it
                    }
                )
            }

            MetodoPagamentoUi.PAYPAL -> {

                InfoPagamentoCard(
                    titolo = "PayPal",
                    testo = "Nella versione demo il pagamento PayPal viene simulato. Non verrai reindirizzato verso un servizio esterno."
                )
            }

            MetodoPagamentoUi.BONIFICO -> {

                InfoPagamentoCard(
                    titolo = "Bonifico bancario",
                    testo = "Nella versione demo non viene effettuato alcun trasferimento reale. La conferma simula il completamento del pagamento."
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Surface(
            color = WarningBackground,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Pagamento simulato: nessun addebito reale verrà effettuato.",
                style = MaterialTheme.typography.bodySmall,
                color = WarningYellow,
                modifier = Modifier.padding(12.dp)
            )
        }

        uiState.errore?.let { errore ->

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = errore,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = onConfermaEPaga,
            enabled = !uiState.isLoading && pagamentoConsentito,
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
                text = if (uiState.isLoading) {
                    "Pagamento in corso..."
                } else {
                    "Conferma e paga"
                },
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun MetodoPagamentoCard(
    titolo: String,
    descrizione: String,
    selezionato: Boolean,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selezionato) {
                3.dp
            } else {
                1.dp
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = selezionato,
                onClick = onClick
            )

            Column(
                modifier = Modifier.padding(start = 6.dp)
            ) {

                Text(
                    text = titolo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Text(
                    text = descrizione,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CartaPagamentoForm(
    numeroCarta: String,
    onNumeroCartaChange: (String) -> Unit,
    intestatario: String,
    onIntestatarioChange: (String) -> Unit,
    scadenza: String,
    onScadenzaChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit
) {

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Dati della carta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            OutlinedTextField(
                value = numeroCarta,

                onValueChange = { nuovoValore ->

                    val cifre = nuovoValore
                        .filter { it.isDigit() }
                        .take(16)

                    val formattato = cifre
                        .chunked(4)
                        .joinToString(" ")

                    onNumeroCartaChange(formattato)
                },
                label = {
                    Text("Numero carta")
                },
                placeholder = {
                    Text("1234 5678 9012 3456")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = intestatario,
                onValueChange = onIntestatarioChange,
                label = {
                    Text("Intestatario")
                },
                placeholder = {
                    Text("Mario Rossi")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = scadenza,

                    onValueChange = { nuovoValore ->

                        val cifre = nuovoValore
                            .filter { it.isDigit() }
                            .take(4)

                        val formattato =
                            if (cifre.length > 2) {
                                "${cifre.take(2)}/${cifre.drop(2)}"
                            } else {
                                cifre
                            }

                        onScadenzaChange(formattato)
                    },

                    label = {
                        Text("Scadenza")
                    },

                    placeholder = {
                        Text("MM/AA")
                    },

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),

                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = cvv,
                    onValueChange = onCvvChange,
                    label = {
                        Text("CVV")
                    },
                    placeholder = {
                        Text("123")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "I dati inseriti sono solo dimostrativi e non vengono inviati al backend.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun InfoPagamentoCard(
    titolo: String,
    testo: String
) {

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = titolo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = testo,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun RigaRiepilogoPagamento(
    etichetta: String,
    valore: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = etichetta,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Text(
            text = valore,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun PagamentoSteps() {

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
            horizontalArrangement = Arrangement.SpaceAround
        ) {

            Text(
                text = "✓ Prenotazione",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "2  Pagamento",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}