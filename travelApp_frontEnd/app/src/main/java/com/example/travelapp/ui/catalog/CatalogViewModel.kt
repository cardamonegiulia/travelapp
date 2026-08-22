package com.example.travelapp.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.ItinerarioRepository
import com.example.travelapp.data.repository.SingolaAttivitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val itinerarioRepository: ItinerarioRepository = ItinerarioRepository(ApiClient.itinerarioApi),
    private val attivitaRepository: SingolaAttivitaRepository = SingolaAttivitaRepository(ApiClient.singolaAttivitaApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        caricaCatalogo()
    }

    fun caricaCatalogo() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val resultItinerari = itinerarioRepository.getAllItinerari()
            val resultAttivita = attivitaRepository.getAllAttivita()

            if (resultItinerari.isSuccess && resultAttivita.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        itinerari = resultItinerari.getOrDefault(emptyList()),
                        attivita = resultAttivita.getOrDefault(emptyList()),
                        errorMessage = null
                    )
                }
            } else {
                val errore = resultItinerari.exceptionOrNull()?.message
                    ?: resultAttivita.exceptionOrNull()?.message
                    ?: "Errore nel caricamento del catalogo"
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = errore)
                }
            }
        }
    }

    fun impostaFiltro(filtro: TipoFiltroCatalogo) {
        _uiState.update { it.copy(filtroSelezionato = filtro) }
    }

    fun impostaRicerca(query: String) {
        _uiState.update { it.copy(queryRicerca = query) }
    }
}