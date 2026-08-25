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

    // --- preferiti ---
    /** Le liste dell'utente, mostrate dal selettore che apre il cuore. */
    val listePreferiti: List<ListaPreferiti> = emptyList(),
    /** Id delle liste che contengono gia' questo itinerario. */
    val listeConItinerario: Set<Long> = emptySet(),
    val preferitiInCaricamento: Boolean = false,
    val selettorePreferitiAperto: Boolean = false,
    val operazionePreferitiInCorso: Boolean = false,
    val messaggioPreferiti: String? = null,
    val errorePreferiti: String? = null
) {
    /** Il cuore e' pieno quando l'itinerario sta in almeno una lista. */
    val ePreferito: Boolean get() = listeConItinerario.isNotEmpty()
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

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun caricaDisponibilitaItinerario(itinerarioId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, idSelezionato = null) }
        viewModelScope.launch {
            val result = itinerarioRepository.getDisponibilitaItinerario(itinerarioId)
            if (result.isSuccess) {
                val list = result.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        disponibilitaItinerario = list,
                        idSelezionato = list.firstOrNull()?.id
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun caricaSessioniAttivita(attivitaId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, idSelezionato = null) }
        viewModelScope.launch {
            val result = attivitaRepository.getSessioniAttivita(attivitaId)
            if (result.isSuccess) {
                val list = result.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessioniAttivita = list,
                        idSelezionato = list.firstOrNull()?.id
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun selezionaSlot(id: Long) {
        _uiState.update { it.copy(idSelezionato = id) }
    }

    // --- preferiti ------------------------------------------------------------------------

    /**
     * Carica le liste dell'utente e segna quelle che gia' contengono l'itinerario.
     *
     * Serve appena si apre la schermata, perche' il cuore deve nascere gia' rosso su un
     * itinerario salvato in precedenza.
     */
    fun caricaPreferiti(itinerarioId: Long) {
        _uiState.update { it.copy(preferitiInCaricamento = true, errorePreferiti = null) }
        viewModelScope.launch { rileggiPreferiti(itinerarioId) }
    }

    fun apriSelettorePreferiti(itinerarioId: Long) {
        _uiState.update {
            it.copy(
                selettorePreferitiAperto = true,
                preferitiInCaricamento = true,
                messaggioPreferiti = null,
                errorePreferiti = null
            )
        }
        // Le liste possono essere cambiate altrove: si rileggono a ogni apertura.
        viewModelScope.launch { rileggiPreferiti(itinerarioId) }
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

    /** Aggiunge o toglie l'itinerario dalla lista toccata nel selettore. */
    fun cambiaAppartenenzaLista(lista: ListaPreferiti, itinerarioId: Long) {
        val eraDentro = lista.id in _uiState.value.listeConItinerario
        val messaggio =
            if (eraDentro) "Rimosso da \"${lista.nome}\"" else "Salvato in \"${lista.nome}\""

        esegui(messaggio, itinerarioId) {
            if (eraDentro) {
                preferitiRepository.rimuoviItinerario(lista.id, itinerarioId).map { }
            } else {
                preferitiRepository.aggiungiItinerario(lista.id, itinerarioId).map { }
            }
        }
    }

    /** Crea una nuova lista privata e ci mette subito dentro l'itinerario. */
    fun creaListaConItinerario(nome: String, itinerarioId: Long) {
        if (nome.isBlank()) {
            _uiState.update { it.copy(errorePreferiti = "Dai un nome alla lista") }
            return
        }

        val nomePulito = nome.trim()
        esegui("Salvato in \"$nomePulito\"", itinerarioId) {
            val creata = preferitiRepository.creaLista(nomePulito, VisibilitaLista.PRIVATA)
            val nuova = creata.getOrNull()
            if (nuova == null) {
                Result.failure(
                    creata.exceptionOrNull() ?: Exception("Errore nella creazione della lista")
                )
            } else {
                preferitiRepository.aggiungiItinerario(nuova.id, itinerarioId).map { }
            }
        }
    }

    fun messaggioPreferitiMostrato() {
        _uiState.update { it.copy(messaggioPreferiti = null, errorePreferiti = null) }
    }

    // --- supporto -------------------------------------------------------------------------

    /**
     * Esegue una scrittura sui preferiti e riallinea lo stato al backend.
     *
     * L'esito non viene ricostruito a mano sulla copia locale: si rilegge, cosi' il cuore
     * e i segni di spunta raccontano quello che il server ha davvero registrato.
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
            val risultato = operazione()

            if (risultato.isFailure) {
                _uiState.update {
                    it.copy(
                        operazionePreferitiInCorso = false,
                        errorePreferiti = risultato.exceptionOrNull()?.message
                    )
                }
                return@launch
            }

            rileggiPreferiti(itinerarioId)
            _uiState.update {
                it.copy(operazionePreferitiInCorso = false, messaggioPreferiti = messaggio)
            }
        }
    }

    /**
     * Rilegge le liste e ricalcola l'appartenenza dell'itinerario.
     *
     * L'elenco `/api/preferiti` restituisce solo i riepiloghi - nome e numero di itinerari,
     * non il contenuto - quindi per sapere dove sta l'itinerario va aperto il dettaglio di
     * ogni lista. Le richieste partono insieme: sono indipendenti e le liste sono poche.
     */
    private suspend fun rileggiPreferiti(itinerarioId: Long) {
        val risultato = preferitiRepository.getMieListe()
        val liste = risultato.getOrNull()

        if (liste == null) {
            _uiState.update {
                it.copy(
                    preferitiInCaricamento = false,
                    errorePreferiti = risultato.exceptionOrNull()?.message
                )
            }
            return
        }

        val contenenti = coroutineScope {
            liste
                .map { lista -> async { lista.id to contieneItinerario(lista.id, itinerarioId) } }
                .awaitAll()
                .filter { (_, contiene) -> contiene }
                .map { (id, _) -> id }
                .toSet()
        }

        _uiState.update {
            it.copy(
                preferitiInCaricamento = false,
                listePreferiti = liste,
                listeConItinerario = contenenti
            )
        }
    }

    private suspend fun contieneItinerario(listaId: Long, itinerarioId: Long): Boolean =
        preferitiRepository.getLista(listaId)
            .getOrNull()
            ?.itinerari
            ?.any { it.id == itinerarioId } == true
}
