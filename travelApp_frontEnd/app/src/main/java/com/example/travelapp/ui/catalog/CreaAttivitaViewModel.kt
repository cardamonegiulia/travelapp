package com.example.travelapp.ui.catalog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.dto.SingolaAttivitaRequestDto
import com.example.travelapp.data.repository.SingolaAttivitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CreaAttivitaUiState(
    val isSalvataggioInCorso: Boolean = false,
    val salvataggioCompletato: Boolean = false,
    val errorMessage: String? = null
)

class CreaAttivitaViewModel(
    private val repository: SingolaAttivitaRepository = SingolaAttivitaRepository(ApiClient.singolaAttivitaApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreaAttivitaUiState())
    val uiState: StateFlow<CreaAttivitaUiState> = _uiState.asStateFlow()

    fun salvaAttivita(
        context: Context,
        idDaModificare: Long?,
        request: SingolaAttivitaRequestDto,
        giorniSettimana: Set<Int>,
        immagineUri: Uri?
    ) {
        _uiState.update { it.copy(isSalvataggioInCorso = true, errorMessage = null) }

        viewModelScope.launch {
            val result = if (idDaModificare != null) {
                repository.updateAttivita(idDaModificare, request)
            } else {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val oggi = Date()
                val cal = Calendar.getInstance().apply {
                    time = oggi
                    add(Calendar.DAY_OF_YEAR, 30)
                }

                val inizio = dateFormat.format(oggi)
                val fine = dateFormat.format(cal.time)

                repository.createAttivitaConSessioni(
                    request = request,
                    inizio = inizio,
                    fine = fine,
                    giorni = giorniSettimana.toList()
                )
            }

            if (result.isSuccess) {
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