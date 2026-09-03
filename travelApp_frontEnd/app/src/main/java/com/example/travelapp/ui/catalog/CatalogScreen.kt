package com.example.travelapp.ui.catalog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.theme.BackgroundLavender
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel = viewModel(),
    onItinerarioClick: (Itinerario) -> Unit = {},
    onAttivitaClick: (SingolaAttivita) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        containerColor = BackgroundLavender,
        topBar = {
            TopAppBar(
                title = { Text("Catalogo Viaggi") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLavender
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.queryRicerca,
                onValueChange = { viewModel.impostaRicerca(it) },
                placeholder = { Text("Cerca itinerari o attività...") },
                leadingIcon = { Text("🔍") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            FiltriRow(
                selezionato = state.filtroSelezionato,
                onFiltroSelected = { viewModel.impostaFiltro(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.errorMessage ?: "Errore", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.caricaCatalogo() }) {
                                Text("Riprova")
                            }
                        }
                    }
                }
                else -> {
                    ListaCatalogo(
                        state = state,
                        onItinerarioClick = onItinerarioClick,
                        onAttivitaClick = onAttivitaClick
                    )
                }
            }
        }
    }
}
@Composable
private fun FiltriRow(
    selezionato: TipoFiltroCatalogo,
    onFiltroSelected: (TipoFiltroCatalogo) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TipoFiltroCatalogo.values()) { filtro ->
            FilterChip(
                selected = selezionato == filtro,
                onClick = { onFiltroSelected(filtro) },
                label = {
                    Text(
                        when (filtro) {
                            TipoFiltroCatalogo.TUTTI -> "Tutti"
                            TipoFiltroCatalogo.ITINERARI -> "Itinerari"
                            TipoFiltroCatalogo.ATTIVITA -> "Attività"
                        }
                    )
                }
            )
        }
    }
}
@Composable
private fun ListaCatalogo(
    state: CatalogUiState,
    onItinerarioClick: (Itinerario) -> Unit,
    onAttivitaClick: (SingolaAttivita) -> Unit
) {
    val query = state.queryRicerca.trim().lowercase()
    val itinerariFiltrati = state.itinerari.filter {
        query.isEmpty() || it.titolo.lowercase().contains(query) || (it.descrizione?.lowercase()?.contains(query) == true)
    }
    val attivitaFiltrate = state.attivita.filter {
        query.isEmpty() || it.titolo.lowercase().contains(query) || (it.luogo?.lowercase()?.contains(query) == true)
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.filtroSelezionato == TipoFiltroCatalogo.TUTTI || state.filtroSelezionato == TipoFiltroCatalogo.ITINERARI) {
            items(itinerariFiltrati) { itinerario ->
                ItinerarioCard(
                    itinerario = itinerario,
                    onClick = { onItinerarioClick(itinerario) }
                )
            }
        }
        if (state.filtroSelezionato == TipoFiltroCatalogo.TUTTI || state.filtroSelezionato == TipoFiltroCatalogo.ATTIVITA) {
            items(attivitaFiltrate) { attivita ->
                SingolaAttivitaCard(
                    attivita = attivita,
                    onClick = { onAttivitaClick(attivita) }
                )
            }
        }
    }
}
