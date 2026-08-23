package com.example.travelapp.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.api.AuthApi
import com.example.travelapp.data.remote.dto.RegistrazioneRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RegistrazioneUiState {
    object Idle : RegistrazioneUiState()
    object Loading : RegistrazioneUiState()
    object Success : RegistrazioneUiState()
    data class Error(val messaggio: String) : RegistrazioneUiState()
}

class RegistrazioneViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<RegistrazioneUiState>(RegistrazioneUiState.Idle)
    val uiState: StateFlow<RegistrazioneUiState> = _uiState

    private val authApi = ApiClient.getClientAutenticato(application)
        .create(AuthApi::class.java)

    fun registra(
        nome: String,
        cognome: String,
        email: String,
        password: String,
        ruolo: String
    ) {
        viewModelScope.launch {
            _uiState.value = RegistrazioneUiState.Loading

            try {
                val risposta = authApi.registra(
                    RegistrazioneRequest(
                        nome = nome,
                        cognome = cognome,
                        email = email,
                        password = password,
                        ruolo = ruolo
                    )
                )

                if (risposta.isSuccessful) {
                    _uiState.value = RegistrazioneUiState.Success
                } else {
                    val errore = when (risposta.code()) {
                        400 -> "Dati non validi — controlla i campi"
                        409 -> "Email già registrata — prova ad accedere"
                        503 -> "Servizio non disponibile — riprova più tardi"
                        else -> "Errore ${risposta.code()}"
                    }
                    _uiState.value = RegistrazioneUiState.Error(errore)
                }
            } catch (e: Exception) {
                _uiState.value = RegistrazioneUiState.Error(
                    "Errore di connessione — verifica la rete"
                )
            }
        }
    }

    fun resetStato() {
        _uiState.value = RegistrazioneUiState.Idle
    }
}