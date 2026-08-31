package com.example.travelapp.ui.catalog

import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.dto.DisponibilitaItinerarioResponseDto
import com.example.travelapp.data.remote.dto.SessioneAttivitaResponseDto
import com.example.travelapp.data.repository.ItinerarioRepository
import com.example.travelapp.data.repository.PreferitiRepository
import com.example.travelapp.data.repository.SingolaAttivitaRepository
import com.example.travelapp.domain.model.ListaPreferiti
import com.example.travelapp.domain.model.VisibilitaLista
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = false,
    val disponibilitaItinerario: List<DisponibilitaItinerarioResponseDto> = emptyList(),
    val sessioniAttivita: List<SessioneAttivitaResponseDto> = emptyList(),
    val idSelezionato: Long? = null,
    val errorMessage: String? = null,

    // Preferiti
    val listePreferiti: List<ListaPreferiti> = emptyList(),
    val listeConItinerario: Set<Long> = emptySet(),
    val preferitiInCaricamento: Boolean = false,
    val selettorePreferitiAperto: Boolean = false,
    val operazionePreferitiInCorso: Boolean = false,
    val messaggioPreferiti: String? = null,
    val errorePreferiti: String? = null
) {

    val ePreferito: Boolean
        get() = listeConItinerario.isNotEmpty()
}

class DetailViewModel(
    application: android.app.Application
) : androidx.lifecycle.AndroidViewModel(application) {

    private val itinerarioRepository =
        ItinerarioRepository(
            ApiClient.getItinerarioApi(application)
        )

    private val attivitaRepository =
        SingolaAttivitaRepository(
            ApiClient.getSingolaAttivitaApi(application)
        )

    private val preferitiRepository =
        PreferitiRepository(
            ApiClient.getPreferitiApi(application)
        )

    private val _uiState =
        MutableStateFlow(DetailUiState())

    val uiState: StateFlow<DetailUiState> =
        _uiState.asStateFlow()

    /*
     * ============================================================
     * DISPONIBILITÀ ITINERARIO
     * ============================================================
     */

    fun caricaDisponibilitaItinerario(
        itinerarioId: Long
    ) {

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                idSelezionato = null
            )
        }

        viewModelScope.launch {

            val result =
                itinerarioRepository
                    .getDisponibilitaItinerario(
                        itinerarioId
                    )

            if (result.isSuccess) {

                val list =
                    result.getOrDefault(
                        emptyList()
                    )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        disponibilitaItinerario = list,
                        idSelezionato =
                            list.firstOrNull()?.id
                    )
                }

            } else {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            result
                                .exceptionOrNull()
                                ?.message
                    )
                }
            }
        }
    }

    /*
     * ============================================================
     * SESSIONI ATTIVITÀ
     * ============================================================
     */

    fun caricaSessioniAttivita(
        attivitaId: Long
    ) {

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                idSelezionato = null
            )
        }

        viewModelScope.launch {

            val result =
                attivitaRepository
                    .getSessioniAttivita(
                        attivitaId
                    )

            if (result.isSuccess) {

                val list =
                    result.getOrDefault(
                        emptyList()
                    )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessioniAttivita = list,
                        idSelezionato =
                            list.firstOrNull()?.id
                    )
                }

            } else {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            result
                                .exceptionOrNull()
                                ?.message
                    )
                }
            }
        }
    }

    fun selezionaSlot(
        id: Long
    ) {

        _uiState.update {
            it.copy(
                idSelezionato = id
            )
        }
    }

    /*
     * ============================================================
     * PREFERITI
     * ============================================================
     */

    fun caricaPreferiti(
        itinerarioId: Long
    ) {

        _uiState.update {
            it.copy(
                preferitiInCaricamento = true,
                errorePreferiti = null
            )
        }

        viewModelScope.launch {
            rileggiPreferiti(
                itinerarioId
            )
        }
    }

    fun apriSelettorePreferiti(
        itinerarioId: Long
    ) {

        _uiState.update {
            it.copy(
                selettorePreferitiAperto = true,
                preferitiInCaricamento = true,
                messaggioPreferiti = null,
                errorePreferiti = null
            )
        }

        viewModelScope.launch {
            rileggiPreferiti(
                itinerarioId
            )
        }
    }

    fun chiudiSelettorePreferiti() {

        _uiState.update {
            it.copy(
                selettorePreferitiAperto = false,
                messaggioPreferiti = null,
                errorePreferiti = null
            )
        }
    }

    fun cambiaAppartenenzaLista(
        lista: ListaPreferiti,
        itinerarioId: Long
    ) {

        val eraDentro =
            lista.id in
                    _uiState
                        .value
                        .listeConItinerario

        val messaggio =
            if (eraDentro) {
                "Rimosso da \"${lista.nome}\""
            } else {
                "Salvato in \"${lista.nome}\""
            }

        esegui(
            messaggio = messaggio,
            itinerarioId = itinerarioId
        ) {

            if (eraDentro) {

                preferitiRepository
                    .rimuoviItinerario(
                        lista.id,
                        itinerarioId
                    )
                    .map { }

            } else {

                preferitiRepository
                    .aggiungiItinerario(
                        lista.id,
                        itinerarioId
                    )
                    .map { }
            }
        }
    }

    fun creaListaConItinerario(
        nome: String,
        itinerarioId: Long
    ) {

        if (nome.isBlank()) {

            _uiState.update {
                it.copy(
                    errorePreferiti =
                        "Dai un nome alla lista"
                )
            }

            return
        }

        val nomePulito =
            nome.trim()

        esegui(
            messaggio =
                "Salvato in \"$nomePulito\"",
            itinerarioId =
                itinerarioId
        ) {

            val creata =
                preferitiRepository
                    .creaLista(
                        nomePulito,
                        VisibilitaLista.PRIVATA
                    )

            val nuova =
                creata.getOrNull()

            if (nuova == null) {

                Result.failure(
                    creata.exceptionOrNull()
                        ?: Exception(
                            "Errore nella creazione della lista"
                        )
                )

            } else {

                preferitiRepository
                    .aggiungiItinerario(
                        nuova.id,
                        itinerarioId
                    )
                    .map { }
            }
        }
    }

    fun messaggioPreferitiMostrato() {

        _uiState.update {
            it.copy(
                messaggioPreferiti = null,
                errorePreferiti = null
            )
        }
    }

    /*
     * ============================================================
     * SUPPORTO PREFERITI
     * ============================================================
     */

    private fun esegui(
        messaggio: String,
        itinerarioId: Long,
        operazione: suspend () -> Result<Unit>
    ) {

        _uiState.update {
            it.copy(
                operazionePreferitiInCorso = true,
                errorePreferiti = null,
                messaggioPreferiti = null
            )
        }

        viewModelScope.launch {

            val risultato =
                operazione()

            if (risultato.isFailure) {

                _uiState.update {
                    it.copy(
                        operazionePreferitiInCorso = false,
                        errorePreferiti =
                            risultato
                                .exceptionOrNull()
                                ?.message
                    )
                }

                return@launch
            }

            rileggiPreferiti(
                itinerarioId
            )

            _uiState.update {
                it.copy(
                    operazionePreferitiInCorso = false,
                    messaggioPreferiti =
                        messaggio
                )
            }
        }
    }

    private suspend fun rileggiPreferiti(
        itinerarioId: Long
    ) {

        val risultato =
            preferitiRepository
                .getMieListe()

        val liste =
            risultato.getOrNull()

        if (liste == null) {

            _uiState.update {
                it.copy(
                    preferitiInCaricamento = false,
                    errorePreferiti =
                        risultato
                            .exceptionOrNull()
                            ?.message
                )
            }

            return
        }

        val contenenti =
            coroutineScope {

                liste
                    .map { lista ->

                        async {

                            lista.id to
                                    contieneItinerario(
                                        lista.id,
                                        itinerarioId
                                    )
                        }
                    }
                    .awaitAll()
                    .filter {
                            (_, contiene) ->
                        contiene
                    }
                    .map {
                            (id, _) ->
                        id
                    }
                    .toSet()
            }

        _uiState.update {
            it.copy(
                preferitiInCaricamento = false,
                listePreferiti = liste,
                listeConItinerario =
                    contenenti
            )
        }
    }

    private suspend fun contieneItinerario(
        listaId: Long,
        itinerarioId: Long
    ): Boolean {

        return preferitiRepository
            .getLista(listaId)
            .getOrNull()
            ?.itinerari
            ?.any {
                it.id == itinerarioId
            } == true
    }
}