package com.example.travelapp.ui.catalog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.ui.theme.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestioneUtentiAdminScreen(
    onBack: () -> Unit,
    viewModel: GestioneUtentiViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.messaggioSuccesso) {
        uiState.messaggioSuccesso?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestione Utenti (Admin)", fontWeight = FontWeight.Bold, color = TravelTextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = TravelTextDark)
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
                text = "Promuovi gli account ad Admin o eliminali dalla piattaforma.",
                color = TravelTextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TravelBlue)
                }
            } else if (uiState.utenti.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun utente registrato", color = TravelTextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.utenti, key = { it.id }) { utente ->
                        val ruoloStr = utente.ruolo.toString().uppercase()
                        val isAdmin = ruoloStr.contains("ADMIN")
                        val isOrganizzatore = ruoloStr.contains("ORGANIZZATORE")
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
                                        .background(if (isAdmin) Color(0xFFFEE2E2) else TravelChipBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isAdmin) Icons.Default.Star else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isAdmin) Color(0xFFDC2626) else TravelBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${utente.nome} ${utente.cognome}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TravelTextDark
                                    )
                                    Text(text = utente.email, fontSize = 13.sp, color = TravelTextMuted)
                                    Spacer(Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when {
                                            isAdmin -> Color(0xFFFEE2E2)
                                            isOrganizzatore -> Color(0xFFFFF7ED)
                                            else -> Color(0xFFEFF6FF)
                                        }
                                    ) {
                                        Text(
                                            text = ruoloStr,
                                            color = when {
                                                isAdmin -> Color(0xFFDC2626)
                                                isOrganizzatore -> TravelOrange
                                                else -> TravelBlue
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (!isAdmin) {
                                        IconButton(
                                            onClick = { viewModel.promuoviAdAdmin(utente.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Rendi Admin",
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.eliminaUtente(utente.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Elimina",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
