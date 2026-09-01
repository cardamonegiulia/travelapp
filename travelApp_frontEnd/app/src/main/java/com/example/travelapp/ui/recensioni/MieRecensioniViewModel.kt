package com.example.travelapp.ui.recensioni

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.RecensioneRepository
import com.example.travelapp.domain.model.Recensione
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Stato dell'elenco "Le mie recensioni".
 *
 * [recensioni] vuoto con [isLoading] a false e nessun [errore] e' il caso "non ho ancora
 * recensito niente": la schermata lo distingue da un errore di rete.
 */
data class MieRecensioniUiState(
    val recensioni: List<Recensione> = emptyList(),
    val isLoading: Boolean = false,
    val errore: String? = null
)

class MieRecensioniViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: RecensioneRepository =
        RecensioneRepository(ApiClient.getRecensioneApi(application))
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MieRecensioniUiState())
    val uiState: StateFlow<MieRecensioniUiState> = _uiState.asStateFlow()

    // Nessun caricamento nell'init: la schermata lo chiede all'ingresso, cosi' rientrando
    // dal form di modifica l'elenco si aggiorna invece di mostrare la versione vecchia.
    fun caricaRecensioni() {
        _uiState.update { it.copy(isLoading = true, errore = null) }

        viewModelScope.launch {
            repository.getMieRecensioni()
                .onSuccess { recensioni ->
                    _uiState.update {
                        it.copy(recensioni = recensioni, isLoading = false)
                    }
                }
                .onFailure { errore ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errore = errore.message
                                ?: "Errore nel caricamento delle recensioni"
                        )
                    }
                }
        }
    }
}
