package com.example.travelapp.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.R
import com.example.travelapp.ui.components.CaricamentoLottie
import com.example.travelapp.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccessViaggiatore: () -> Unit,
    onLoginSuccessOrganizzatore: () -> Unit,
    onVaiRegistrazione: () -> Unit,
    emailPreCompilata: String? = null,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { intent ->
            viewModel.gestisciRispostaLogin(intent)
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                if (state.ruolo == "ORGANIZZATORE") {
                    onLoginSuccessOrganizzatore()
                } else {
                    onLoginSuccessViaggiatore()
                }
                viewModel.resetStato()
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        if (!emailPreCompilata.isNullOrBlank()) {
            launcher.launch(viewModel.creaIntentLogin(emailPreCompilata))
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
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo con gradiente
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryBlue, PrimaryDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Se sta caricando mostra l'animazione nel logo
                if (uiState is AuthUiState.Loading) {
                    CaricamentoLottie(dimensione = 70.dp, animazione = R.raw.flight)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Titolo animato
            AnimatedContent(
                targetState = uiState is AuthUiState.Loading,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { isLoading ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isLoading) "Accesso in corso..." else "TravelApp",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLoading) "Attendere prego" else "Scopri il mondo con noi",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // Card login
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bentornato!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Accedi con il tuo account",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pulsante login con animazione
                    Button(
                        onClick = {
                            val intent = viewModel.creaIntentLogin(emailPreCompilata)
                            launcher.launch(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            disabledContainerColor = PrimaryBlue.copy(alpha = 0.6f)
                        ),
                        enabled = uiState !is AuthUiState.Loading
                    ) {
                        AnimatedContent(
                            targetState = uiState is AuthUiState.Loading,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            }
                        ) { isLoading ->
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CaricamentoLottie(dimensione = 28.dp, animazione = R.raw.flight)
                                    Text(
                                        text = "Accesso...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = "Accedi",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Messaggio errore animato
                    AnimatedVisibility(
                        visible = uiState is AuthUiState.Error,
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
                                    text = if (uiState is AuthUiState.Error)
                                        (uiState as AuthUiState.Error).messaggio
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

            // Link registrazione
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Non hai un account? ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onVaiRegistrazione,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Registrati",
                        color = PrimaryBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}