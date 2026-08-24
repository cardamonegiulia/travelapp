package com.example.travelapp.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.dto.DisponibilitaItinerarioResponseDto
import com.example.travelapp.data.remote.dto.SessioneAttivitaResponseDto
import com.example.travelapp.data.repository.ItinerarioRepository
import com.example.travelapp.data.repository.SingolaAttivitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = false,
    val disponibilitaItinerario: List<DisponibilitaItinerarioResponseDto> = emptyList(),
    val sessioniAttivita: List<SessioneAttivitaResponseDto> = emptyList(),
    val idSelezionato: Long? = null,
    val errorMessage: String? = null
)

class DetailViewModel(
    private val itinerarioRepository: ItinerarioRepository = ItinerarioRepository(ApiClient.itinerarioApi),
    private val attivitaRepository: SingolaAttivitaRepository = SingolaAttivitaRepository(ApiClient.singolaAttivitaApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun caricaDisponibilitaItinerario(itinerarioId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, idSelezionato = null) }
        viewModelScope.launch {
            val result = itinerarioRepository.getDisponibilitaItinerario(itinerarioId)
            if (result.isSuccess) {
                val list = result.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        disponibilitaItinerario = list,
                        idSelezionato = list.firstOrNull()?.id
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun caricaSessioniAttivita(attivitaId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, idSelezionato = null) }
        viewModelScope.launch {
            val result = attivitaRepository.getSessioniAttivita(attivitaId)
            if (result.isSuccess) {
                val list = result.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessioniAttivita = list,
                        idSelezionato = list.firstOrNull()?.id
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun selezionaSlot(id: Long) {
        _uiState.update { it.copy(idSelezionato = id) }
    }
}