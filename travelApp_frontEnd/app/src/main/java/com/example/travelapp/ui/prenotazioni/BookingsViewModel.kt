package com.example.travelapp.ui.prenotazioni

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.NotificaRepository
import com.example.travelapp.data.repository.PagamentoRepository
import com.example.travelapp.data.repository.PrenotazioneRepository
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.domain.model.StatoPagamento
import com.example.travelapp.domain.model.StatoPrenotazione
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Le due schede della sezione prenotazioni.
 */
enum class SchedaPrenotazioni {
    ATTUALI,
    CONCLUSI
}

data class BookingsListUiState(
    val prenotazioni: List<Prenotazione> = emptyList(),
    val viaggiConclusi: List<Prenotazione> = emptyList(),
    val schedaSelezionata: SchedaPrenotazioni = SchedaPrenotazioni.ATTUALI,
    val prenotazioneSelezionata: Prenotazione? = null,
    val notificheNonLette: Long = 0,
    val isLoading: Boolean = false,
    val errore: String? = null
)

class BookingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        PrenotazioneRepository(
            ApiClient.getPrenotazioneApi(application)
        )

    private val pagamentoRepository =
        PagamentoRepository(
            ApiClient.getPagamentoApi(application)
        )

    private val notificaRepository =
        NotificaRepository(
            ApiClient.getNotificaApi(application)
        )

    private val _uiState =
        MutableStateFlow(
            BookingsListUiState()
        )

    val uiState: StateFlow<BookingsListUiState> =
        _uiState.asStateFlow()

    init {
        caricaPrenotazioni()
    }

    /**
     * Carica sia le prenotazioni attuali/future
     * sia i viaggi conclusi.
     *
     * La divisione viene effettuata dal backend.
     */
    fun caricaPrenotazioni() {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errore = null
                )
            }

            try {

                val attuali =
                    repository
                        .getPrenotazioniAttuali()

                val conclusi =
                    repository
                        .getViaggiConclusi()

                _uiState.update {
                    it.copy(
                        prenotazioni = attuali,
                        viaggiConclusi = conclusi,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errore =
                            e.message
                                ?: "Errore nel caricamento delle prenotazioni"
                    )
                }
            }
        }

        aggiornaNotifiche()
    }

    /**
     * Aggiorna il numero di notifiche non lette.
     */
    fun aggiornaNotifiche() {

        viewModelScope.launch {

            notificaRepository
                .contaNonLette()
                .onSuccess { quante ->

                    _uiState.update {
                        it.copy(
                            notificheNonLette = quante
                        )
                    }
                }
        }
    }

    /**
     * Cambia fra prenotazioni attuali e viaggi conclusi.
     */
    fun selezionaScheda(
        scheda: SchedaPrenotazioni
    ) {

        _uiState.update {
            it.copy(
                schedaSelezionata = scheda,
                prenotazioneSelezionata = null,
                errore = null
            )
        }
    }

    /**
     * Apre il dettaglio di una prenotazione.
     */
    fun selezionaPrenotazione(
        prenotazione: Prenotazione
    ) {

        _uiState.update {
            it.copy(
                prenotazioneSelezionata =
                    prenotazione,
                errore = null
            )
        }
    }

    /**
     * Chiude il dettaglio.
     */
    fun chiudiDettaglio() {

        _uiState.update {
            it.copy(
                prenotazioneSelezionata = null,
                errore = null
            )
        }
    }

    /**
     * Annulla la prenotazione selezionata.
     */
    fun annullaPrenotazione() {

        val prenotazione =
            _uiState
                .value
                .prenotazioneSelezionata
                ?: return

        if (_uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errore = null
                )
            }

            try {

                val aggiornata =
                    repository
                        .annullaPrenotazione(
                            prenotazione.id
                        )

                _uiState.update { stato ->

                    stato.copy(

                        prenotazioni =
                            stato.prenotazioni.map {
                                if (
                                    it.id ==
                                    aggiornata.id
                                ) {
                                    aggiornata
                                } else {
                                    it
                                }
                            },

                        viaggiConclusi =
                            stato.viaggiConclusi.map {
                                if (
                                    it.id ==
                                    aggiornata.id
                                ) {
                                    aggiornata
                                } else {
                                    it
                                }
                            },

                        prenotazioneSelezionata =
                            aggiornata,

                        isLoading = false
                    )
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errore =
                            e.message
                                ?: "Errore durante l'annullamento"
                    )
                }
            }
        }
    }

    /**
     * Completa il pagamento di una prenotazione
     * rimasta IN_ATTESA.
     */
    fun completaPagamento() {

        val prenotazione =
            _uiState
                .value
                .prenotazioneSelezionata
                ?: return

        if (_uiState.value.isLoading) {
            return
        }

        if (
            prenotazione.statoPrenotazione !=
            StatoPrenotazione.IN_ATTESA ||
            prenotazione.statoPagamento !=
            StatoPagamento.IN_ATTESA
        ) {

            _uiState.update {
                it.copy(
                    errore =
                        "Questa prenotazione non ha un pagamento in attesa."
                )
            }

            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errore = null
                )
            }

            try {

                pagamentoRepository
                    .pagaPrenotazione(
                        prenotazione.id
                    )

                /*
                 * Rileggiamo la prenotazione
                 * direttamente dal backend.
                 *
                 * In questo modo recuperiamo gli stati
                 * aggiornati:
                 *
                 * Prenotazione -> CONFERMATA
                 * Pagamento    -> COMPLETATO
                 */
                val aggiornata =
                    repository
                        .getPrenotazione(
                            prenotazione.id
                        )

                _uiState.update { stato ->

                    stato.copy(

                        prenotazioni =
                            stato.prenotazioni.map {
                                if (
                                    it.id ==
                                    aggiornata.id
                                ) {
                                    aggiornata
                                } else {
                                    it
                                }
                            },

                        viaggiConclusi =
                            stato.viaggiConclusi.map {
                                if (
                                    it.id ==
                                    aggiornata.id
                                ) {
                                    aggiornata
                                } else {
                                    it
                                }
                            },

                        prenotazioneSelezionata =
                            aggiornata,

                        isLoading = false
                    )
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errore =
                            e.message
                                ?: "Errore durante il pagamento"
                    )
                }
            }
        }
    }
}