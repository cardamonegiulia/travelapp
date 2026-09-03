package com.example.travelapp.ui.catalog
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.PrenotazioneRepository
import com.example.travelapp.domain.model.PartenzaOrganizzatore
import com.example.travelapp.domain.model.PrenotatoPartenza
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class PartenzeUiState(
    val itinerarioId: Long? = null,
    val titoloItinerario: String = "",
    val partenze: List<PartenzaOrganizzatore> = emptyList(),
    val isLoading: Boolean = false,
    val errore: String? = null,
    val partenzaDaEliminare: PartenzaOrganizzatore? = null,
    val eliminazioneInCorso: Boolean = false,
    val messaggio: String? = null
)
data class PrenotatiUiState(
    val partenza: PartenzaOrganizzatore? = null,
    val prenotati: List<PrenotatoPartenza> = emptyList(),
    val isLoading: Boolean = false,
    val errore: String? = null
)
class PartenzeOrganizzatoreViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = PrenotazioneRepository(
        ApiClient.getPrenotazioneApi(application)
    )
    private val _partenze = MutableStateFlow(PartenzeUiState())
    val partenze: StateFlow<PartenzeUiState> = _partenze.asStateFlow()
    private val _prenotati = MutableStateFlow(PrenotatiUiState())
    val prenotati: StateFlow<PrenotatiUiState> = _prenotati.asStateFlow()
    fun caricaPartenze(itinerarioId: Long, titoloItinerario: String) {
        _partenze.update {
            it.copy(
                itinerarioId = itinerarioId,
                titoloItinerario = titoloItinerario,
                isLoading = true,
                errore = null
            )
        }
        viewModelScope.launch {
            try {
                val risultato = repository.getPartenzeItinerario(itinerarioId)
                _partenze.update {
                    it.copy(
                        partenze = risultato,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _partenze.update {
                    it.copy(
                        isLoading = false,
                        errore = e.message ?: "Errore imprevisto"
                    )
                }
            }
        }
    }
    fun chiediConfermaEliminazione(partenza: PartenzaOrganizzatore) {
        _partenze.update { it.copy(partenzaDaEliminare = partenza) }
    }
    fun annullaEliminazione() {
        _partenze.update { it.copy(partenzaDaEliminare = null) }
    }
    fun confermaEliminazione() {
        val partenza = _partenze.value.partenzaDaEliminare ?: return
        _partenze.update {
            it.copy(
                partenzaDaEliminare = null,
                eliminazioneInCorso = true
            )
        }
        viewModelScope.launch {
            val esito = repository.eliminaPartenza(partenza.disponibilitaId)
            if (esito.isSuccess) {
                _partenze.update { stato ->
                    stato.copy(
                        partenze = stato.partenze.filterNot {
                            it.disponibilitaId == partenza.disponibilitaId
                        },
                        eliminazioneInCorso = false,
                        messaggio = "Partenza eliminata"
                    )
                }
            } else {
                _partenze.update {
                    it.copy(
                        eliminazioneInCorso = false,
                        messaggio = esito.exceptionOrNull()?.message
                            ?: "Impossibile eliminare la partenza"
                    )
                }
            }
        }
    }
    fun messaggioMostrato() {
        _partenze.update { it.copy(messaggio = null) }
    }
    fun ricaricaPartenze() {
        val stato = _partenze.value
        stato.itinerarioId?.let { caricaPartenze(it, stato.titoloItinerario) }
    }
    fun caricaPrenotati(partenza: PartenzaOrganizzatore) {
        _prenotati.value = PrenotatiUiState(
            partenza = partenza,
            isLoading = true
        )
        viewModelScope.launch {
            try {
                val risultato = repository.getPrenotatiPartenza(partenza.disponibilitaId)
                _prenotati.update {
                    it.copy(
                        prenotati = risultato,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _prenotati.update {
                    it.copy(
                        isLoading = false,
                        errore = e.message ?: "Errore imprevisto"
                    )
                }
            }
        }
    }
    fun ricaricaPrenotati() {
        _prenotati.value.partenza?.let { caricaPrenotati(it) }
    }
}
