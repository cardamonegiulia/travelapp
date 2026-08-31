package com.example.travelapp.ui.prenotazioni

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.data.repository.ItinerarioRepository
import com.example.travelapp.data.repository.PagamentoRepository
import com.example.travelapp.data.repository.PrenotazioneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class PrenotazioniViewModel(
    private val prenotazioneRepository: PrenotazioneRepository,
    private val pagamentoRepository: PagamentoRepository,
    private val itinerarioRepository: ItinerarioRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(BookingUiState())

    val uiState: StateFlow<BookingUiState> =
        _uiState.asStateFlow()

    /*
     * ============================================================
     * TOTALE
     * ============================================================
     */

    private fun ricalcolaTotale() {

        val stato =
            _uiState.value

        val prezzoBase =
            stato.prezzoBaseUnitario *
                    stato.numeroPartecipanti

        val prezzoExtraUnitario =
            stato.extraSelezionati
                .values
                .sum()

        val prezzoExtra =
            prezzoExtraUnitario *
                    stato.numeroPartecipanti

        val totale =
            prezzoBase +
                    prezzoExtra

        _uiState.value =
            stato.copy(
                prezzoBase = prezzoBase,
                prezzoExtra = prezzoExtra,
                prezzoTotaleVisualizzato = totale
            )
    }

    /*
     * ============================================================
     * INIZIALIZZAZIONE BOOKING
     * ============================================================
     */

    fun inizializzaBooking(
        titolo: String,
        luogo: String,
        prezzoBaseUnitario: Double,
        itinerarioId: Long? = null,
        disponibilitaItinerarioId: Long? = null,
        sessioneSingolaAttivitaId: Long? = null,
        dataInizio: String? = null,
        dataFine: String? = null,
        postiDisponibili: Int? = null
    ) {

        /*
         * Creiamo un nuovo stato invece di fare copy
         * sul booking precedente.
         *
         * Così extra, pagamento ed errori della prenotazione
         * precedente non rimangono nella nuova.
         */
        _uiState.value =
            BookingUiState(
                titolo = titolo,
                luogo = luogo,
                prezzoBaseUnitario = prezzoBaseUnitario,
                prezzoBase = prezzoBaseUnitario,
                prezzoTotaleVisualizzato =
                    prezzoBaseUnitario,

                itinerarioId =
                    itinerarioId,

                disponibilitaItinerarioId =
                    disponibilitaItinerarioId,

                sessioneSingolaAttivitaId =
                    sessioneSingolaAttivitaId,

                dataInizio =
                    dataInizio,

                dataFine =
                    dataFine,

                postiDisponibili =
                    postiDisponibili
            )

        /*
         * Gli extra esistono soltanto per un itinerario.
         * Le singole attività non devono effettuare questa chiamata.
         */
        if (itinerarioId != null) {
            caricaExtra(
                itinerarioId
            )
        }
    }

    fun resetBooking() {

        _uiState.value =
            BookingUiState()
    }

    fun pulisciErrore() {

        _uiState.value =
            _uiState.value.copy(
                errore = null
            )
    }

    /*
     * ============================================================
     * PARTECIPANTI
     * ============================================================
     */

    fun incrementaPartecipanti() {

        val stato =
            _uiState.value

        val massimo =
            stato.postiDisponibili

        /*
         * Non permettiamo alla UI di superare
         * i posti della partenza/sessione scelta.
         */
        if (
            massimo != null &&
            stato.numeroPartecipanti >= massimo
        ) {
            return
        }

        _uiState.value =
            stato.copy(
                numeroPartecipanti =
                    stato.numeroPartecipanti + 1
            )

        ricalcolaTotale()
    }

    fun decrementaPartecipanti() {

        if (
            _uiState.value.numeroPartecipanti > 1
        ) {

            _uiState.value =
                _uiState.value.copy(
                    numeroPartecipanti =
                        _uiState.value.numeroPartecipanti - 1
                )

            ricalcolaTotale()
        }
    }

    /*
     * ============================================================
     * EXTRA
     * ============================================================
     */

    fun toggleExtra(
        attivitaId: Long,
        prezzoUnitario: Double
    ) {

        val stato =
            _uiState.value

        val nuoviExtra =
            stato.extraSelezionati
                .toMutableMap()

        if (
            attivitaId in nuoviExtra
        ) {

            nuoviExtra.remove(
                attivitaId
            )

        } else {

            nuoviExtra[
                attivitaId
            ] = prezzoUnitario
        }

        _uiState.value =
            stato.copy(
                extraSelezionati =
                    nuoviExtra
            )

        ricalcolaTotale()
    }

    /*
     * ============================================================
     * CREAZIONE PRENOTAZIONE
     * ============================================================
     */

    fun creaPrenotazione() {

        if (_uiState.value.isLoading) {
            return
        }

        val stato =
            _uiState.value

        /*
         * Deve essere valorizzato esattamente uno fra:
         *
         * - disponibilitaItinerarioId
         * - sessioneSingolaAttivitaId
         */
        if (
            (stato.disponibilitaItinerarioId == null) ==
            (stato.sessioneSingolaAttivitaId == null)
        ) {

            _uiState.value =
                stato.copy(
                    errore =
                        "Selezione della prenotazione non valida"
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errore = null
                )

            try {

                val request =
                    CreaPrenotazioneDto(
                        disponibilitaItinerarioId =
                            stato.disponibilitaItinerarioId,

                        sessioneSingolaAttivitaId =
                            stato.sessioneSingolaAttivitaId,

                        numeroPartecipanti =
                            stato.numeroPartecipanti,

                        attivitaExtraIds =
                            stato.attivitaExtraIds
                    )

                val prenotazione =
                    prenotazioneRepository
                        .creaPrenotazione(
                            request
                        )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        prenotazioneCreata =
                            prenotazione
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errore =
                            messaggioErrore(
                                e,
                                "Errore durante la prenotazione"
                            )
                    )
            }
        }
    }

    /*
     * ============================================================
     * PAGAMENTO
     * ============================================================
     */

    fun pagaPrenotazione() {

        if (_uiState.value.isLoading) {
            return
        }

        val prenotazione =
            _uiState.value
                .prenotazioneCreata

        if (prenotazione == null) {

            _uiState.value =
                _uiState.value.copy(
                    errore =
                        "Nessuna prenotazione da pagare"
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errore = null
                )

            try {

                val pagamento =
                    pagamentoRepository
                        .pagaPrenotazione(
                            prenotazione.id
                        )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        pagamentoCompletato =
                            pagamento
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errore =
                            messaggioErrore(
                                e,
                                "Errore durante il pagamento"
                            )
                    )
            }
        }
    }

    fun selezionaMetodoPagamento(
        metodo: MetodoPagamentoUi
    ) {

        _uiState.value =
            _uiState.value.copy(
                metodoPagamento =
                    metodo
            )
    }

    /*
     * ============================================================
     * CARICAMENTO EXTRA
     * ============================================================
     */

    private fun caricaExtra(
        itinerarioId: Long
    ) {

        _uiState.value =
            _uiState.value.copy(
                extraInCaricamento = true,
                errore = null
            )

        viewModelScope.launch {

            val risultato =
                itinerarioRepository
                    .getAttivitaExtra(
                        itinerarioId
                    )

            /*
             * L'utente potrebbe aver aperto un'altra
             * prenotazione mentre questa richiesta era
             * ancora in corso.
             *
             * In quel caso non aggiorniamo il nuovo booking
             * con gli extra del precedente.
             */
            if (
                _uiState.value.itinerarioId !=
                itinerarioId
            ) {
                return@launch
            }

            if (risultato.isSuccess) {

                val extra =
                    risultato
                        .getOrDefault(
                            emptyList()
                        )
                        .map { dto ->

                            ExtraUi(
                                id = dto.id,
                                nome = dto.titolo,
                                prezzo =
                                    dto.prezzoExtra
                                        .toDouble()
                            )
                        }

                _uiState.value =
                    _uiState.value.copy(
                        extraDisponibili =
                            extra,

                        extraInCaricamento =
                            false
                    )

            } else {

                _uiState.value =
                    _uiState.value.copy(
                        extraDisponibili =
                            emptyList(),

                        extraInCaricamento =
                            false,

                        errore =
                            risultato
                                .exceptionOrNull()
                                ?.message
                                ?: "Errore nel caricamento degli extra"
                    )
            }
        }
    }

    /*
     * ============================================================
     * ERRORI HTTP
     * ============================================================
     */

    /**
     * Traduce gli errori Retrofit in messaggi
     * comprensibili per l'utente.
     */
    private fun messaggioErrore(
        e: Exception,
        ripiego: String
    ): String {

        if (e !is HttpException) {
            return e.message
                ?: ripiego
        }

        return when (
            e.code()
        ) {

            400 ->
                "Dati della prenotazione non validi"

            401 ->
                "Sessione scaduta: effettua di nuovo l'accesso"

            403 ->
                "Non hai i permessi per completare questa operazione"

            404 ->
                "Offerta non più disponibile"

            409 ->
                "Posti esauriti oppure prenotazioni chiuse per questa data"

            429 ->
                "Troppe richieste ravvicinate: riprova fra qualche istante"

            else ->
                ripiego
        }
    }
}