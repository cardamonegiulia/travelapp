package com.example.travelapp.ui.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.ItinerarioRepository
import com.example.travelapp.data.repository.SingolaAttivitaRepository
import com.example.travelapp.data.repository.UtenteRepository
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
    application: Application
) : AndroidViewModel(application) {

    private val itinerarioRepository =
        ItinerarioRepository(
            ApiClient.getItinerarioApi(application)
        )

    private val attivitaRepository =
        SingolaAttivitaRepository(
            ApiClient.getSingolaAttivitaApi(application)
        )

    private val utenteRepository =
        UtenteRepository(application)

    private val _uiState = MutableStateFlow(OfferteUiState())
    val uiState: StateFlow<OfferteUiState> = _uiState.asStateFlow()

    fun caricaOfferte(soloMie: Boolean = false) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val resItinerari = itinerarioRepository.getAllItinerari()
            val resAttivita = attivitaRepository.getAllAttivita()

            var listaItinerari = resItinerari.getOrDefault(emptyList())
            var listaAttivita = resAttivita.getOrDefault(emptyList())

            if (soloMie) {
                val profiloRes = utenteRepository.caricaProfilo()
                val utente = profiloRes.getOrNull()

                if (utente != null) {
                    val mioId = utente.id
                    listaItinerari = listaItinerari.filter { it.organizzatoreId == mioId }
                    listaAttivita = listaAttivita.filter { it.organizzatoreId == mioId }
                } else {
                    listaItinerari = emptyList()
                    listaAttivita = emptyList()
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
                _uiState.update {
                    it.copy(errorMessage = "Errore eliminazione itinerario")
                }
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
                _uiState.update {
                    it.copy(errorMessage = "Errore eliminazione attività")
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update {
            it.copy(
                feedbackMessage = null,
                errorMessage = null
            )
        }
    }
}