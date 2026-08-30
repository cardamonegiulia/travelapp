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

// A Success il backend ha già chiuso tutte le sessioni: il chiamante deve riportare al login.
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
