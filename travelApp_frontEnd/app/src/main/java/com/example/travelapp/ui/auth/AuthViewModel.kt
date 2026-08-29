package com.example.travelapp.ui.auth

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.KeycloakManager
import com.example.travelapp.data.remote.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val ruolo: String) : AuthUiState
    data class Error(val messaggio: String) : AuthUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun creaIntentLogin(loginHint: String? = null, forzaLogin: Boolean = false): Intent {
        return KeycloakManager.creaIntentLogin(getApplication(), loginHint, forzaLogin)
    }

    fun gestisciRispostaLogin(intent: Intent) {
        _uiState.value = AuthUiState.Loading

        KeycloakManager.scambiaCodicePToken(
            context = getApplication(),
            intent = intent,
            onSuccess = { accessToken ->
                viewModelScope.launch {
                    val ruolo = KeycloakManager.estraiRuolo(accessToken)
                    val nome = KeycloakManager.estraiNome(accessToken)
                    val email = KeycloakManager.estraiEmail(accessToken)

                    TokenManager.salvaToken(
                        context = getApplication(),
                        token = accessToken,
                        ruolo = ruolo,
                        nome = nome,
                        email = email
                    )

                    _uiState.value = AuthUiState.Success(ruolo = ruolo)
                }
            },
            onError = { errore ->
                _uiState.value = AuthUiState.Error(messaggio = errore)
            }
        )
    }

    fun resetStato() {
        _uiState.value = AuthUiState.Idle
    }
}