package com.example.travelapp.ui.auth

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.KeycloakManager
import com.example.travelapp.data.remote.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Tutti i possibili stati della schermata di login/registrazione
sealed class AuthUiState {
    object Idle : AuthUiState()           // stato iniziale — niente sta succedendo
    object Loading : AuthUiState()        // sto caricando
    data class Success(
        val ruolo: String                 // login riuscito — so il ruolo dell'utente
    ) : AuthUiState()
    data class Error(
        val messaggio: String             // qualcosa è andato storto
    ) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    /**
     * Crea l'Intent per aprire Keycloak nel browser.
     * La schermata di login lo lancia con startActivityForResult.
     */
    fun creaIntentLogin(loginHint: String? = null, forzaLogin: Boolean = false): Intent {
        return KeycloakManager.creaIntentLogin(getApplication(), loginHint, forzaLogin)
    }

    /**
     * Chiamato dopo che Keycloak ha rimandato l'utente all'app.
     * Scambia il codice con il token e salva tutto.
     */
    fun gestisciRispostaLogin(intent: Intent) {
        _uiState.value = AuthUiState.Loading

        KeycloakManager.scambiaCodicePToken(
            context = getApplication(),
            intent = intent,
            onSuccess = { sessione ->
                viewModelScope.launch {
                    val ruolo = KeycloakManager.estraiRuolo(sessione.accessToken)
                    TokenManager.salvaSessione(
                        context = getApplication(),
                        accessToken = sessione.accessToken,
                        refreshToken = sessione.refreshToken,
                        scadenzaMs = sessione.scadenzaMs,
                        ruolo = ruolo
                    )
                    _uiState.value = AuthUiState.Success(ruolo = ruolo)
                }
            },
            onError = { errore ->
                _uiState.value = AuthUiState.Error(messaggio = errore)
            }
        )
    }

    /**
     * Resetta lo stato — utile quando l'utente vuole riprovare
     * dopo un errore.
     */
    fun resetStato() {
        _uiState.value = AuthUiState.Idle
    }

}