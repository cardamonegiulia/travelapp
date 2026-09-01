package com.example.travelapp.ui.catalog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.dto.ItinerarioRequestDto
import com.example.travelapp.data.repository.ItinerarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreaItinerarioUiState(
    val isSalvataggioInCorso: Boolean = false,
    val salvataggioCompletato: Boolean = false,
    val errorMessage: String? = null
)

class CreaItinerarioViewModel(
    application: android.app.Application
) : AndroidViewModel(application) {

    private val repository =
        ItinerarioRepository(
            ApiClient.getItinerarioApi(application)
        )

    private val _uiState = MutableStateFlow(CreaItinerarioUiState())
    val uiState: StateFlow<CreaItinerarioUiState> = _uiState.asStateFlow()

    fun salvaItinerario(
        context: Context,
        idDaModificare: Long?,
        request: ItinerarioRequestDto,
        immaginiUri: List<Uri>
    ) {
        _uiState.update { it.copy(isSalvataggioInCorso = true, errorMessage = null) }

        viewModelScope.launch {
            val result = if (idDaModificare != null) {
                repository.updateItinerario(idDaModificare, request)
            } else {
                repository.createItinerario(request)
            }

            if (result.isSuccess) {
                val itinerarioSalvato = result.getOrNull()

                // Upload di tutte le immagini selezionate
                if (itinerarioSalvato != null && immaginiUri.isNotEmpty()) {
                    immaginiUri.forEach { uri ->
                        repository.caricaImmagine(context, itinerarioSalvato.id, uri)
                    }
                }

                _uiState.update { it.copy(isSalvataggioInCorso = false, salvataggioCompletato = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSalvataggioInCorso = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Errore durante il salvataggio"
                    )
                }
            }
        }
    }

    fun resetStato() {
        _uiState.value = CreaItinerarioUiState()
    }
}