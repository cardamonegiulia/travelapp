package com.example.travelapp.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onVaiOfferte: () -> Unit,
    onVaiUtenti: () -> Unit,
    onVaiProfilo: () -> Unit = {},
    onLogout: () -> Unit,
    homeViewModel: OrganizzatoreHomeViewModel = viewModel()
) {
    val saldo by homeViewModel.saldo.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.caricaSaldo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pannello Amministrazione",
                        fontWeight = FontWeight.Bold,
                        color = TravelTextDark
                    )
                },
                actions = {
                    IconButton(onClick = onVaiProfilo) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profilo",
                            tint = TravelBlue
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = LogoutRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TravelBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card del Saldo
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TravelBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Saldo Guadagnato Piattaforma",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "€ ${saldo?.toPlainString() ?: "0.00"}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onVaiOfferte,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelBlue)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Tutte le offerte della piattaforma", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onVaiUtenti,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IconPurple)
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Gestione utenti registrati", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}