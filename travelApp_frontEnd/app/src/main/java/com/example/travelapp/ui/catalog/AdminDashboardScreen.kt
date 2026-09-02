package com.example.travelapp.ui.catalog

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.PrenotazioneRepository
import com.example.travelapp.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class AdminDashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo = PrenotazioneRepository(
        ApiClient.getPrenotazioneApi(application)
    )
    private val _saldo = MutableStateFlow<BigDecimal?>(null)
    val saldo: StateFlow<BigDecimal?> = _saldo.asStateFlow()

    init {
        caricaSaldo()
    }

    fun caricaSaldo() {
        viewModelScope.launch {
            val result = repo.getSaldoTotaleGlobale()
            _saldo.value = result.getOrDefault(BigDecimal.ZERO)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onVaiOfferte: () -> Unit,
    onVaiUtenti: () -> Unit,
    onVaiProfilo: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel()
) {
    val saldo by viewModel.saldo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pannello Amministratore", fontWeight = FontWeight.Bold, color = TravelTextDark) },
                actions = {
                    IconButton(onClick = onVaiProfilo) {
                        Icon(
                            Icons.Default.AccountBox,
                            contentDescription = "Profilo",
                            tint = TravelBlue
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Saldo Globale
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TravelBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Saldo Totale Piattaforma", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "€ ${saldo?.toPlainString() ?: "..."}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text("Gestione Globale", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TravelTextDark)

            // Card Offerte Globali
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TravelSurface),
                elevation = CardDefaults.cardElevation(2.dp),
                onClick = onVaiOfferte,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.List, contentDescription = null, tint = TravelBlue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Tutte le Offerte", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TravelTextDark)
                        Text("Visualizza ed elimina itinerari e attività del catalogo", fontSize = 13.sp, color = TravelTextMuted)
                    }
                }
            }

            // Card Utenti
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TravelSurface),
                elevation = CardDefaults.cardElevation(2.dp),
                onClick = onVaiUtenti,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBox, contentDescription = null, tint = TravelOrange, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Gestione Utenti", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TravelTextDark)
                        Text("Visualizza gli utenti e promuovili ad Amministratore", fontSize = 13.sp, color = TravelTextMuted)
                    }
                }
            }
        }
    }
}