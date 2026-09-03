package com.example.travelapp.ui.catalog
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.repository.UtenteRepository
import com.example.travelapp.domain.model.Utente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class GestioneUtentiUiState(
    val isLoading: Boolean = false,
    val utenti: List<Utente> = emptyList(),
    val messaggioSuccesso: String? = null,
    val errorMessage: String? = null
)
class GestioneUtentiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UtenteRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(GestioneUtentiUiState())
    val uiState: StateFlow<GestioneUtentiUiState> = _uiState.asStateFlow()
    init {
        caricaUtenti()
    }
    fun caricaUtenti() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.getTuttiGliUtenti()
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, utenti = result.getOrDefault(emptyList())) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }
    fun promuoviAdAdmin(id: Long) {
        viewModelScope.launch {
            val result = repository.promuoviAdAdmin(id)
            if (result.isSuccess) {
                val utenteAggiornato = result.getOrNull()
                _uiState.update { state ->
                    state.copy(
                        utenti = state.utenti.map { utente ->
                            if (utente.id == id) (utenteAggiornato ?: utente) else utente
                        },
                        messaggioSuccesso = "Utente promosso ad Admin!"
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Errore durante la promozione") }
            }
        }
    }
    fun eliminaUtente(id: Long) {
        viewModelScope.launch {
            val result = repository.eliminaUtente(id)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        utenti = state.utenti.filterNot { it.id == id },
                        messaggioSuccesso = "Utente eliminato con successo!"
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Errore durante l'eliminazione dell'utente") }
            }
        }
    }
    fun clearFeedback() {
        _uiState.update { it.copy(messaggioSuccesso = null, errorMessage = null) }
    }
}
