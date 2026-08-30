package com.example.travelapp.ui.catalog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.dto.SingolaAttivitaRequestDto
import com.example.travelapp.data.repository.SingolaAttivitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreaAttivitaUiState(
    val isSalvataggioInCorso: Boolean = false,
    val salvataggioCompletato: Boolean = false,
    val errorMessage: String? = null
)

class CreaAttivitaViewModel(
    application: android.app.Application
) : AndroidViewModel(application) {

    private val repository =
        SingolaAttivitaRepository(
            ApiClient.getSingolaAttivitaApi(application)
        )

    private val _uiState = MutableStateFlow(CreaAttivitaUiState())
    val uiState: StateFlow<CreaAttivitaUiState> = _uiState.asStateFlow()

    fun salvaAttivita(
        context: Context,
        idDaModificare: Long?,
        request: SingolaAttivitaRequestDto,
        dataInizio: String,
        dataFine: String,
        giorniSettimana: Set<Int>,
        immagineUri: Uri?
    ) {
        _uiState.update { it.copy(isSalvataggioInCorso = true, errorMessage = null) }

        viewModelScope.launch {
            val result = if (idDaModificare != null) {
                repository.updateAttivita(idDaModificare, request)
            } else {
                repository.createAttivitaConSessioni(
                    request = request,
                    inizio = dataInizio,
                    fine = dataFine,
                    giorni = giorniSettimana.toList().sorted()
                )
            }

            if (result.isSuccess) {
                val attivitaSalvata = result.getOrNull()
                if (immagineUri != null && attivitaSalvata != null) {
                    repository.caricaImmagine(context, attivitaSalvata.id, immagineUri)
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
        _uiState.value = CreaAttivitaUiState()
    }
}