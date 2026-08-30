package com.example.travelapp.ui.profilo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.repository.UtenteRepository
import com.example.travelapp.domain.model.Utente
import com.example.travelapp.ui.screens.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Logica di presentazione del profilo: pubblica [ProfileUiState].
 *
 * È un `AndroidViewModel` perché la foto arriva come `content://` e per leggerla serve il
 * `ContentResolver`, cioè un `Context`. Quello dell'applicazione, non quello della
 * Activity: il caricamento deve poter finire anche se lo schermo ruota.
 */
class ProfiloViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UtenteRepository(application)

    private val _state = MutableStateFlow(statoIniziale())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        caricaProfilo()
    }

    /**
     * Rilegge il profilo dal backend, foto compresa.
     *
     * Se la chiamata fallisce lo stato resta com'è: senza login (nessun token in `TokenManager`) la
     * risposta è 401, e svuotare la schermata per questo non aiuterebbe nessuno.
     */
    fun caricaProfilo() {
        viewModelScope.launch {
            repository.caricaProfilo().onSuccess { utente -> _state.update { it.conProfilo(utente) } }
        }
    }

    /**
     * Carica sul backend la foto scelta dal photo picker.
     *
     * L'avatar passa subito alla foto locale, prima che l'upload finisca: l'utente ha già
     * espresso la scelta e non ha senso mostrargli il segnaposto mentre aspetta. Se il
     * caricamento fallisce si torna alla foto precedente, così quello che si vede
     * corrisponde sempre a quello che il server ha davvero.
     */
    fun cambiaFotoProfilo(uri: Uri) {
        val precedente = _state.value.avatarUrl

        _state.update {
            it.copy(avatarUrl = uri.toString(), isPhotoUploading = true, photoMessage = null)
        }

        viewModelScope.launch {
            repository.impostaFotoProfilo(uri)
                .onSuccess { utente ->
                    _state.update {
                        it.conProfilo(utente).copy(
                            isPhotoUploading = false,
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

    // Cambia subito a schermo; se il salvataggio fallisce torna al valore precedente.
    fun cambiaTemaScuro(attivo: Boolean) {
        val precedente = _state.value.isDarkModeEnabled
        val id = _state.value.id

        _state.update { it.copy(isDarkModeEnabled = attivo) }

        if (id == null) return

        viewModelScope.launch {
            repository.aggiornaTema(id, attivo)
                .onFailure { _state.update { it.copy(isDarkModeEnabled = precedente) } }
        }
    }

    /** Da chiamare dopo aver mostrato [ProfileUiState.photoMessage], perché non riappaia. */
    fun messaggioMostrato() {
        _state.update { it.copy(photoMessage = null) }
    }

    private fun ProfileUiState.conProfilo(utente: Utente) = copy(
        id = utente.id,
        name = utente.nomeCompleto,
        email = utente.email,
        ruolo = utente.ruolo ?: ruolo,
        avatarUrl = utente.fotoProfiloUrl,
        isDarkModeEnabled = utente.tema?.equals("SCURO", ignoreCase = true) ?: isDarkModeEnabled
    )

    /**
     * Dati di esempio finché il login non esiste: senza token il backend risponde 401 e la
     * schermata resterebbe vuota. Vengono sostituiti dal profilo vero al primo
     * [caricaProfilo] andato a buon fine.
     */
    private fun statoIniziale() = ProfileUiState(
        name = "Mario Rossi",
        email = "mario@example.it",
        avatarUrl = null,
        isDarkModeEnabled = false
    )
}
