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


/** Partenze di un itinerario: la lista da cui l'organizzatore sceglie il periodo. */
data class PartenzeUiState(
    val itinerarioId: Long? = null,
    val titoloItinerario: String = "",
    val partenze: List<PartenzaOrganizzatore> = emptyList(),
    val isLoading: Boolean = false,
    val errore: String? = null
)

/** Chi ha comprato una singola partenza. */
data class PrenotatiUiState(
    val partenza: PartenzaOrganizzatore? = null,
    val prenotati: List<PrenotatoPartenza> = emptyList(),
    val isLoading: Boolean = false,
    val errore: String? = null
)


/**
 * Alimenta le due schermate con cui l'organizzatore vede chi ha comprato un suo itinerario:
 * l'elenco delle partenze e, per la partenza scelta, l'elenco dei prenotati.
 *
 * Un solo ViewModel per entrambe perche' la seconda schermata mostra in testata la partenza
 * scelta nella prima: tenerle separate significherebbe rileggerla dalla rete.
 */
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

    /** Ricarica l'itinerario gia' aperto: serve al pulsante "Riprova". */
    fun ricaricaPartenze() {
        val stato = _partenze.value
        stato.itinerarioId?.let { caricaPartenze(it, stato.titoloItinerario) }
    }

    /**
     * La partenza arriva gia' scelta dalla schermata precedente, cosi' la testata dei
     * prenotati puo' mostrare date e posti senza attendere la risposta.
     */
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

    /** Ricarica l'ultima partenza aperta, per lo stesso motivo. */
    fun ricaricaPrenotati() {
        _prenotati.value.partenza?.let { caricaPrenotati(it) }
    }
}
