package com.example.travelapp.ui.recensioni

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.RecensioneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecensioneUiState(
    val prenotazioneId: Long? = null,
    val titoloViaggio: String = "",
    val votazione: Int = 0,
    val commento: String = "",
    val recensioneEsistenteId: Long? = null,
    val isLoading: Boolean = false,
    val isSalvataggio: Boolean = false,
    val salvata: Boolean = false,
    val errore: String? = null
) {
    val isModifica: Boolean get() = recensioneEsistenteId != null
    val puoSalvare: Boolean get() = votazione in 1..5 && !isSalvataggio && !isLoading
}

class RecensioneViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: RecensioneRepository =
        RecensioneRepository(ApiClient.getRecensioneApi(application))
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecensioneUiState())
    val uiState: StateFlow<RecensioneUiState> = _uiState.asStateFlow()

    fun apri(prenotazioneId: Long, titoloViaggio: String) {
        _uiState.value = RecensioneUiState(
            prenotazioneId = prenotazioneId,
            titoloViaggio = titoloViaggio,
            isLoading = true
        )

        viewModelScope.launch {
            val risultato = repository.getRecensionePrenotazione(prenotazioneId)
            val esistente = risultato.getOrNull()

            _uiState.update { stato ->
                stato.copy(
                    isLoading = false,
                    recensioneEsistenteId = esistente?.id,
                    votazione = esistente?.votazione ?: 0,
                    commento = esistente?.commento.orEmpty(),
                    errore = null
                )
            }
        }
    }

    fun impostaVotazione(stelle: Int) {
        _uiState.update { it.copy(votazione = stelle, errore = null) }
    }

    fun impostaCommento(testo: String) {
        _uiState.update { it.copy(commento = testo) }
    }

    fun salva() {
        val stato = _uiState.value
        val prenotazioneId = stato.prenotazioneId ?: return

        if (stato.votazione !in 1..5) {
            _uiState.update { it.copy(errore = "Scegli quante stelle dare al viaggio") }
            return
        }
        if (stato.isSalvataggio) return

        _uiState.update { it.copy(isSalvataggio = true, errore = null) }

        viewModelScope.launch {
            val risultato = if (stato.recensioneEsistenteId != null) {
                repository.aggiornaRecensione(
                    stato.recensioneEsistenteId,
                    stato.votazione,
                    stato.commento
                )
            } else {
                repository.creaRecensione(
                    prenotazioneId,
                    stato.votazione,
                    stato.commento
                )
            }

            risultato
                .onSuccess { recensione ->
                    _uiState.update {
                        it.copy(
                            isSalvataggio = false,
                            salvata = true,
                            recensioneEsistenteId = recensione.id
                        )
                    }
                }
                .onFailure { errore ->
                    _uiState.update {
                        it.copy(
                            isSalvataggio = false,
                            errore = errore.message ?: "Errore durante il salvataggio"
                        )
                    }
                }
        }
    }


    fun reset() {
        _uiState.value = RecensioneUiState()
    }
}
