package com.example.travelapp.ui.prenotazioni

import androidx.lifecycle.ViewModel
import com.example.travelapp.data.repository.PrenotazioneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.data.repository.PagamentoRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException

class PrenotazioniViewModel(
    private val prenotazioneRepository: PrenotazioneRepository,
    private val pagamentoRepository: PagamentoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())

    private fun ricalcolaTotale() {
        val stato = _uiState.value

        val prezzoBase =
            stato.prezzoBaseUnitario * stato.numeroPartecipanti

        val prezzoExtraUnitario =
            stato.extraSelezionati.values.sum()

        val prezzoExtra =
            prezzoExtraUnitario * stato.numeroPartecipanti

        val totale =
            prezzoBase + prezzoExtra

        _uiState.value = stato.copy(
            prezzoBase = prezzoBase,
            prezzoExtra = prezzoExtra,
            prezzoTotaleVisualizzato = totale
        )
    }

    val uiState: StateFlow<BookingUiState> =
        _uiState.asStateFlow()

    fun inizializzaBooking(
        titolo: String,
        luogo: String,
        prezzoBaseUnitario: Double
    ) {
        _uiState.value = _uiState.value.copy(
            titolo = titolo,
            luogo = luogo,
            prezzoBaseUnitario = prezzoBaseUnitario,
            prezzoBase = prezzoBaseUnitario,
            prezzoTotaleVisualizzato = prezzoBaseUnitario
        )
    }

    fun resetBooking() {
        _uiState.value = BookingUiState()
    }

    fun pulisciErrore() {
        _uiState.value = _uiState.value.copy(
            errore = null
        )
    }

    fun incrementaPartecipanti() {
        _uiState.value = _uiState.value.copy(
            numeroPartecipanti = _uiState.value.numeroPartecipanti + 1
        )
        ricalcolaTotale()
    }
    fun decrementaPartecipanti() {
        if (_uiState.value.numeroPartecipanti > 1) {
            _uiState.value = _uiState.value.copy(
                numeroPartecipanti = _uiState.value.numeroPartecipanti - 1
            )
            ricalcolaTotale()
        }
    }

    fun toggleExtra(
        attivitaId: Long,
        prezzoUnitario: Double
    ) {
        val stato = _uiState.value

        val nuoviExtra =
            stato.extraSelezionati.toMutableMap()

        if (attivitaId in nuoviExtra) {
            nuoviExtra.remove(attivitaId)
        } else {
            nuoviExtra[attivitaId] = prezzoUnitario
        }

        _uiState.value = stato.copy(
            extraSelezionati = nuoviExtra
        )

        ricalcolaTotale()
    }

    fun creaPrenotazione(
        disponibilitaItinerarioId: Long? = null,
        sessioneSingolaAttivitaId: Long? = null
    ) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errore = null
            )

            try {
                val request = CreaPrenotazioneDto(
                    disponibilitaItinerarioId = disponibilitaItinerarioId,
                    sessioneSingolaAttivitaId = sessioneSingolaAttivitaId,
                    numeroPartecipanti = _uiState.value.numeroPartecipanti,
                    attivitaExtraIds = _uiState.value.attivitaExtraIds
                )

                val prenotazione =
                    prenotazioneRepository.creaPrenotazione(request)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    prenotazioneCreata = prenotazione
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errore = messaggioErrore(e, "Errore durante la prenotazione")
                )
            }
        }
    }

    fun pagaPrenotazione() {
        if (_uiState.value.isLoading) return

        val prenotazione = _uiState.value.prenotazioneCreata

        if (prenotazione == null) {
            _uiState.value = _uiState.value.copy(
                errore = "Nessuna prenotazione da pagare"
            )
            return
        }

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errore = null
            )

            try {
                val pagamento =
                    pagamentoRepository.pagaPrenotazione(prenotazione.id)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pagamentoCompletato = pagamento
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errore = messaggioErrore(e, "Errore durante il pagamento")
                )
            }
        }
    }
    fun selezionaMetodoPagamento(
        metodo: MetodoPagamentoUi
    ) {
        _uiState.value = _uiState.value.copy(
            metodoPagamento = metodo
        )
    }

    /**
     * Traduce l'errore HTTP in una frase leggibile.
     *
     * Senza questa mappatura l'utente si vede scritto "HTTP 409 Conflict", che non dice
     * ne' cos'e' andato storto ne' cosa puo' fare: proprio i due casi piu' frequenti
     * (posti finiti, prenotazioni chiuse) arrivano con quel codice.
     */
    private fun messaggioErrore(
        e: Exception,
        ripiego: String
    ): String {

        if (e !is HttpException) {
            return e.message ?: ripiego
        }

        return when (e.code()) {
            400 -> "Dati della prenotazione non validi"
            401 -> "Sessione scaduta: effettua di nuovo l'accesso"
            403 -> "Non hai i permessi per completare questa operazione"
            404 -> "Offerta non più disponibile"
            409 -> "Posti esauriti oppure prenotazioni chiuse per questa data"
            429 -> "Troppe richieste ravvicinate: riprova fra qualche istante"
            else -> ripiego
        }
    }
}
