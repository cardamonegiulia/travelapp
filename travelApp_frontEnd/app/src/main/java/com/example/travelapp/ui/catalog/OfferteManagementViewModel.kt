package com.example.travelapp.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.ItinerarioRepository
import com.example.travelapp.data.repository.SingolaAttivitaRepository
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OfferteUiState(
    val isLoading: Boolean = false,
    val itinerari: List<Itinerario> = emptyList(),
    val attivita: List<SingolaAttivita> = emptyList(),
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class OfferteManagementViewModel(
    private val itinerarioRepository: ItinerarioRepository = ItinerarioRepository(ApiClient.itinerarioApi),
    private val attivitaRepository: SingolaAttivitaRepository = SingolaAttivitaRepository(ApiClient.singolaAttivitaApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfferteUiState())
    val uiState: StateFlow<OfferteUiState> = _uiState.asStateFlow()

    fun caricaOfferte(soloMie: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val resItinerari = itinerarioRepository.getAllItinerari()
            val resAttivita = attivitaRepository.getAllAttivita()

            var listaItinerari = resItinerari.getOrDefault(emptyList())
            var listaAttivita = resAttivita.getOrDefault(emptyList())

            if (soloMie) {
                // Recupera l'id dell'utente corrente dall'API sincronizzaProfilo
                val profiloRes = runCatching { ApiClient.utenteApi.sincronizzaProfilo() }
                val mioId = profiloRes.getOrNull()?.body()?.id
                if (mioId != null) {
                    listaItinerari = listaItinerari.filter { it.organizzatoreId == mioId }
                    listaAttivita = listaAttivita.filter { it.organizzatoreId == mioId }
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    itinerari = listaItinerari,
                    attivita = listaAttivita
                )
            }
        }
    }

    fun eliminaItinerario(id: Long) {
        viewModelScope.launch {
            val result = itinerarioRepository.deleteItinerario(id)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        itinerari = state.itinerari.filterNot { it.id == id },
                        feedbackMessage = "Itinerario eliminato con successo"
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Errore eliminazione itinerario") }
            }
        }
    }

    fun eliminaAttivita(id: Long) {
        viewModelScope.launch {
            val result = attivitaRepository.deleteAttivita(id)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        attivita = state.attivita.filterNot { it.id == id },
                        feedbackMessage = "Attività eliminata con successo"
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Errore eliminazione attività") }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }
}