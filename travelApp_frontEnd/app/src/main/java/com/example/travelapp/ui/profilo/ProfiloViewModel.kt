package com.example.travelapp.ui.profilo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.KeycloakManager
import com.example.travelapp.data.remote.TokenManager
import com.example.travelapp.data.repository.UtenteRepository
import com.example.travelapp.domain.model.Utente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfiloViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UtenteRepository(application)

    private val _state = MutableStateFlow(ProfiloUiState())
    val state: StateFlow<ProfiloUiState> = _state.asStateFlow()

    init {
        inizializzaDaCache()
        caricaProfilo()
    }

    private fun inizializzaDaCache() {
        viewModelScope.launch {
            val nome = TokenManager.getNome(getApplication()).firstOrNull().orEmpty()
            val email = TokenManager.getEmail(getApplication()).firstOrNull().orEmpty()
            val ruolo = TokenManager.getRuolo(getApplication()).firstOrNull() ?: "VIAGGIATORE"

            if (nome.isNotBlank() || email.isNotBlank()) {
                _state.update {
                    it.copy(
                        name = nome,
                        email = email,
                        ruolo = ruolo
                    )
                }
            }
        }
    }

    fun caricaProfilo() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.caricaProfilo()
                .onSuccess { utente ->
                    val nomeValido = if (utente.nomeCompleto.isNotBlank()) utente.nomeCompleto else utente.nome
                    val emailValida = utente.email

                    _state.update {
                        it.copy(
                            isLoading = false,
                            utente = utente,
                            name = nomeValido.ifBlank { it.name },
                            email = emailValida.ifBlank { it.email },
                            ruolo = utente.ruolo ?: it.ruolo,
                            avatarUrl = utente.fotoProfiloUrl,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { errore ->
                    val token = TokenManager.getToken(getApplication()).firstOrNull()
                    if (!token.isNullOrBlank()) {
                        val nomeJwt = KeycloakManager.estraiNome(token)
                        val emailJwt = KeycloakManager.estraiEmail(token)
                        val ruoloJwt = KeycloakManager.estraiRuolo(token)

                        _state.update {
                            it.copy(
                                isLoading = false,
                                name = if (nomeJwt.isNotBlank()) nomeJwt else it.name,
                                email = if (emailJwt.isNotBlank()) emailJwt else it.email,
                                ruolo = ruoloJwt,
                                errorMessage = null
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, errorMessage = errore.message) }
                    }
                }
        }
    }

    fun cambiaFotoProfilo(uri: Uri) {
        val precedente = _state.value.avatarUrl

        _state.update {
            it.copy(avatarUrl = uri.toString(), isPhotoUploading = true, photoMessage = null)
        }

        viewModelScope.launch {
            repository.impostaFotoProfilo(uri)
                .onSuccess { utente ->
                    _state.update {
                        it.copy(
                            isPhotoUploading = false,
                            utente = utente,
                            avatarUrl = utente.fotoProfiloUrl,
                            photoMessage = "Foto profilo aggiornata"
                        )
                    }
                }
                .onFailure { errore ->
                    _state.update {
                        it.copy(
                            avatarUrl = precedente,
                            isPhotoUploading = false,
                            photoMessage = errore.message ?: "Caricamento non riuscito"
                        )
                    }
                }
        }
    }

    fun rimuoviFotoProfilo() {
        val precedente = _state.value.avatarUrl
        _state.update { it.copy(avatarUrl = null, isPhotoUploading = true, photoMessage = null) }

        viewModelScope.launch {
            repository.rimuoviFotoProfilo()
                .onSuccess {
                    _state.update {
                        it.copy(isPhotoUploading = false, photoMessage = "Foto profilo rimossa")
                    }
                }
                .onFailure { errore ->
                    _state.update {
                        it.copy(
                            avatarUrl = precedente,
                            isPhotoUploading = false,
                            photoMessage = errore.message ?: "Rimozione non riuscita"
                        )
                    }
                }
        }
    }

    fun cambiaTemaScuro(attivo: Boolean) {
        _state.update { it.copy(isDarkModeEnabled = attivo) }
    }

    fun messaggioMostrato() {
        _state.update { it.copy(photoMessage = null) }
    }
}