package com.example.travelapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.profilo.CambiaPasswordUiState
import com.example.travelapp.ui.profilo.CambiaPasswordViewModel
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.DividerColor
import com.example.travelapp.ui.theme.ErrorRed
import com.example.travelapp.ui.theme.LogoutBackground
import com.example.travelapp.ui.theme.LogoutRed
import com.example.travelapp.ui.theme.PrimaryBlue
import com.example.travelapp.ui.theme.SuccessGreen
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary


@Composable
fun CambiaPasswordScreen(
    onBack: () -> Unit,
    onPasswordCambiata: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CambiaPasswordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var nuovaPassword by remember { mutableStateOf("") }
    var confermaPassword by remember { mutableStateOf("") }
    var nuovaPasswordVisibile by remember { mutableStateOf(false) }
    var confermaPasswordVisibile by remember { mutableStateOf(false) }

    val lunghezzaOk = nuovaPassword.length in 12..128
    val contenutoOk = nuovaPassword.any { it.isLetter() } && nuovaPassword.any { it.isDigit() }
    val passwordValida = lunghezzaOk && contenutoOk
    val passwordCoincidono = confermaPassword.isNotEmpty() && confermaPassword == nuovaPassword
    val formValido = passwordValida && passwordCoincidono

    LaunchedEffect(uiState) {
        if (uiState is CambiaPasswordUiState.Success) {
            onPasswordCambiata()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        topBar = { AppTopBar(title = "Cambia password", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        text = "Scegli la nuova password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nuovaPassword,
                        onValueChange = { nuovaPassword = it },
                        label = { Text("Nuova password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = uiState !is CambiaPasswordUiState.Loading,
                        visualTransformation = if (nuovaPasswordVisibile) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { nuovaPasswordVisibile = !nuovaPasswordVisibile }) {
                                Text(
                                    text = if (nuovaPasswordVisibile) "Nascondi" else "Mostra",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = campoColori()
                    )

                    AnimatedVisibility(
                        visible = nuovaPassword.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
                            RequisitoPassword(
                                testo = "Almeno 12 caratteri",
                                soddisfatto = lunghezzaOk
                            )
                            RequisitoPassword(
                                testo = "Almeno una lettera e un numero",
                                soddisfatto = contenutoOk
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confermaPassword,
                        onValueChange = { confermaPassword = it },
                        label = { Text("Ripeti nuova password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = uiState !is CambiaPasswordUiState.Loading,
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
                        onClick = { viewModel.cambiaPassword(nuovaPassword) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            disabledContainerColor = PrimaryBlue.copy(alpha = 0.4f)
                        ),
                        enabled = formValido && uiState !is CambiaPasswordUiState.Loading
                    ) {
                        AnimatedContent(
                            targetState = uiState is CambiaPasswordUiState.Loading,
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { isLoading ->
                            Text(
                                text = if (isLoading) "Salvataggio…" else "Salva nuova password",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState is CambiaPasswordUiState.Error,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LogoutBackground)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = (uiState as? CambiaPasswordUiState.Error)?.messaggio.orEmpty(),
                                    color = LogoutRed,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState is CambiaPasswordUiState.Success,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Password aggiornata: effettua di nuovo l'accesso",
                                color = SuccessGreen,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
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
