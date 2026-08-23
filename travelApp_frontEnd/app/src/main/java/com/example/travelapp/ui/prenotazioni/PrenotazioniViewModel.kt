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

class PrenotazioniViewModel(
    private val prenotazioneRepository: PrenotazioneRepository,
    private val pagamentoRepository: PagamentoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())

    val uiState: StateFlow<BookingUiState> =
        _uiState.asStateFlow()

    fun inizializzaBooking(
        titolo: String,
        luogo: String,
        prezzoBase: Double
    ) {
        _uiState.value = _uiState.value.copy(
            titolo = titolo,
            luogo = luogo,
            prezzoBase = prezzoBase,
            prezzoTotaleVisualizzato = prezzoBase
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
    }

    fun decrementaPartecipanti() {
        if (_uiState.value.numeroPartecipanti > 1) {
            _uiState.value = _uiState.value.copy(
                numeroPartecipanti = _uiState.value.numeroPartecipanti - 1
            )
        }
    }

    fun toggleExtra(attivitaId: Long) {
        val extraAttuali = _uiState.value.attivitaExtraIds

        val nuoviExtra = if (attivitaId in extraAttuali) {
            extraAttuali - attivitaId
        } else {
            extraAttuali + attivitaId
        }

        _uiState.value = _uiState.value.copy(
            attivitaExtraIds = nuoviExtra
        )
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
                    errore = e.message ?: "Errore durante la prenotazione"
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
                    errore = e.message ?: "Errore durante il pagamento"
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
}
