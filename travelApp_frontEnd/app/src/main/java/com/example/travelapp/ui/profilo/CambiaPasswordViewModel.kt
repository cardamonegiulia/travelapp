package com.example.travelapp.ui.profilo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.repository.UtenteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CambiaPasswordUiState {
    object Idle : CambiaPasswordUiState()
    object Loading : CambiaPasswordUiState()
    object Success : CambiaPasswordUiState()
    data class Error(val messaggio: String) : CambiaPasswordUiState()
}

/**
 * Se il cambio password va a buon fine, il backend chiude tutte le sessioni dell'utente
 * (vedi `UtenteController.cambiaPassword`): il token attuale smette di essere valido, quindi
 * dopo [CambiaPasswordUiState.Success] il chiamante deve riportare l'utente al login, non
 * solo mostrare un messaggio.
 */
class CambiaPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UtenteRepository(application)

    private val _uiState = MutableStateFlow<CambiaPasswordUiState>(CambiaPasswordUiState.Idle)
    val uiState: StateFlow<CambiaPasswordUiState> = _uiState

    fun cambiaPassword(nuovaPassword: String) {
        viewModelScope.launch {
            _uiState.value = CambiaPasswordUiState.Loading

            repository.cambiaPassword(nuovaPassword)
                .onSuccess {
                    _uiState.value = CambiaPasswordUiState.Success
                }
                .onFailure { errore ->
                    _uiState.value = CambiaPasswordUiState.Error(
                        errore.message ?: "Cambio password non riuscito"
                    )
                }
        }
    }

    fun resetStato() {
        _uiState.value = CambiaPasswordUiState.Idle
    }
}
