package com.example.travelapp.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.theme.*

data class UtenteAdminItem(
    val id: Long,
    val nome: String,
    val email: String,
    val ruolo: String // "VIAGGIATORE", "ORGANIZZATORE", "ADMIN"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestioneUtentiAdminScreen(
    utenti: List<UtenteAdminItem>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestione Utenti", fontWeight = FontWeight.Bold, color = TravelTextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = TravelTextDark)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = "Cerca", tint = TravelTextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TravelBg)
                .padding(16.dp)
        ) {
            Text(
                text = "Visualizza e gestisci gli account della piattaforma.",
                color = TravelTextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(utenti, key = { it.id }) { utente ->
                    val isOrganizzatore = utente.ruolo.contains("ORGANIZZATORE", ignoreCase = true)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TravelSurface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(TravelChipBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = TravelBlue, modifier = Modifier.size(24.dp))
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = utente.nome, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TravelTextDark)
                                Text(text = utente.email, fontSize = 13.sp, color = TravelTextMuted)
                            }

                            // Badge Ruolo
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOrganizzatore) Color(0xFFFFF7ED) else Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = if (isOrganizzatore) "Organizzatore" else "Viaggiatore",
                                    color = if (isOrganizzatore) TravelOrange else TravelBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}