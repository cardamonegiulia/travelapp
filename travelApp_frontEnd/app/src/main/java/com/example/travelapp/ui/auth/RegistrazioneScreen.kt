package com.example.travelapp.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.ui.components.CaricamentoLottie
import com.example.travelapp.ui.theme.*

@Composable
fun RegistrazioneScreen(
    onRegistrazioneSuccess: (email: String) -> Unit,
    onVaiLogin: () -> Unit,
    viewModel: RegistrazioneViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confermaPassword by remember { mutableStateOf("") }
    var ruoloSelezionato by remember { mutableStateOf("VIAGGIATORE") }
    var passwordVisibile by remember { mutableStateOf(false) }
    var confermaPasswordVisibile by remember { mutableStateOf(false) }

    val passwordLunghezzaOk = password.length >= 12
    val passwordContenutoOk = password.any { it.isLetter() } && password.any { it.isDigit() }
    val passwordValida = passwordLunghezzaOk && passwordContenutoOk
    val passwordCoincidono = confermaPassword.isNotEmpty() && confermaPassword == password

    val formValido = nome.isNotBlank() && cognome.isNotBlank() &&
            email.isNotBlank() && passwordValida && passwordCoincidono

    LaunchedEffect(uiState) {
        if (uiState is RegistrazioneUiState.Success) {
            onRegistrazioneSuccess(email)
            viewModel.resetStato()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryBlue, PrimaryDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Crea account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scegli come vuoi usare TravelApp",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RuoloOption(
                            icona = Icons.AutoMirrored.Filled.Send,
                            etichetta = "Viaggiatore",
                            selezionato = ruoloSelezionato == "VIAGGIATORE",
                            onClick = { ruoloSelezionato = "VIAGGIATORE" },
                            modifier = Modifier.weight(1f)
                        )
                        RuoloOption(
                            icona = Icons.Filled.Settings,
                            etichetta = "Organizzatore",
                            selezionato = ruoloSelezionato == "ORGANIZZATORE",
                            onClick = { ruoloSelezionato = "ORGANIZZATORE" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = campoColori()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = cognome,
                        onValueChange = { cognome = it },
                        label = { Text("Cognome") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = campoColori()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        colors = campoColori()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisibile) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisibile = !passwordVisibile }) {
                                Text(
                                    text = if (passwordVisibile) "Nascondi" else "Mostra",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = campoColori()
                    )

                    AnimatedVisibility(
                        visible = password.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
                            RequisitoPassword(
                                testo = "Almeno 12 caratteri",
                                soddisfatto = passwordLunghezzaOk
                            )
                            RequisitoPassword(
                                testo = "Almeno una lettera e un numero",
                                soddisfatto = passwordContenutoOk
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confermaPassword,
                        onValueChange = { confermaPassword = it },
                        label = { Text("Ripeti password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = confermaPassword.isNotEmpty() && !passwordCoincidono,
                        visualTransformation = if (confermaPasswordVisibile) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { confermaPasswordVisibile = !confermaPasswordVisibile }) {
                                Text(
                                    text = if (confermaPasswordVisibile) "Nascondi" else "Mostra",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = campoColori()
                    )

                    AnimatedVisibility(
                        visible = confermaPassword.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (passwordCoincidono) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                contentDescription = null,
                                tint = if (passwordCoincidono) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (passwordCoincidono) "Le password coincidono" else "Le password non coincidono",
                                fontSize = 12.sp,
                                color = if (passwordCoincidono) SuccessGreen else ErrorRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.registra(
                                nome = nome,
                                cognome = cognome,
                                email = email,
                                password = password,
                                ruolo = ruoloSelezionato
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            disabledContainerColor = PrimaryBlue.copy(alpha = 0.4f)
                        ),
                        enabled = uiState !is RegistrazioneUiState.Loading && formValido
                    ) {
                        AnimatedContent(
                            targetState = uiState is RegistrazioneUiState.Loading,
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { isLoading ->
                            if (isLoading) {
                                CaricamentoLottie(dimensione = 28.dp)
                            } else {
                                Text(
                                    text = if (ruoloSelezionato == "ORGANIZZATORE")
                                        "Registrati come Organizzatore"
                                    else
                                        "Registrati come Viaggiatore",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState is RegistrazioneUiState.Error,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LogoutBackground)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (uiState is RegistrazioneUiState.Error)
                                        (uiState as RegistrazioneUiState.Error).messaggio
                                    else "",
                                    color = LogoutRed,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hai già un account? ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onVaiLogin,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Accedi",
                        color = PrimaryBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RuoloOption(
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    etichetta: String,
    selezionato: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selezionato) BadgeBlue else BackgroundLight)
            .border(
                width = if (selezionato) 2.dp else 1.dp,
                color = if (selezionato) PrimaryBlue else DividerColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icona,
            contentDescription = null,
            tint = if (selezionato) PrimaryBlue else TextSecondary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = etichetta,
            fontSize = 12.sp,
            fontWeight = if (selezionato) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selezionato) PrimaryDark else TextSecondary
        )
    }
}

@Composable
private fun RequisitoPassword(testo: String, soddisfatto: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (soddisfatto) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TextSecondary)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = testo,
            fontSize = 12.sp,
            color = if (soddisfatto) SuccessGreen else TextSecondary
        )
    }
}

@Composable
private fun campoColori() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = DividerColor,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = PrimaryBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
