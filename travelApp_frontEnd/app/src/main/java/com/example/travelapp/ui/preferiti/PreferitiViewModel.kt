package com.example.travelapp.ui.preferiti

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.PreferitiRepository
import com.example.travelapp.domain.model.ListaPreferiti
import com.example.travelapp.domain.model.VisibilitaLista
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Stato della schermata "Preferiti": le liste dell'utente e quelle condivise con lui.
 *
 * E' un `AndroidViewModel` perche' le API sotto `/api` viaggiano tutte sul client
 * autenticato, e per leggere il token dal DataStore serve un Context.
 *
 * `@JvmOverloads` non e' decorativo: la factory di default di `viewModel()` cerca per
 * reflection un costruttore che prenda il solo `Application`, e i parametri con valore di
 * default in Kotlin non ne generano uno.
 */
class PreferitiViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PreferitiRepository =
        PreferitiRepository(ApiClient.getPreferitiApi(application))
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PreferitiUiState())
    val state: StateFlow<PreferitiUiState> = _state.asStateFlow()

    init {
        carica()
    }

    /**
     * Ricarica entrambe le sezioni.
     *
     * Le due chiamate sono indipendenti e vengono valutate separatamente: se il backend
     * risponde su una sola, mostrare comunque quella e' meglio che svuotare la schermata.
     */
    fun carica() {
        _state.update { it.copy(isLoading = true, errore = null) }
        viewModelScope.launch {
            val mie = repository.getMieListe()
            val condivise = repository.getListeCondiviseConMe()

            _state.update { stato ->
                stato.copy(
                    isLoading = false,
                    mieListe = mie.getOrDefault(stato.mieListe),
                    condiviseConMe = condivise.getOrDefault(stato.condiviseConMe),
                    errore = mie.exceptionOrNull()?.message ?: condivise.exceptionOrNull()?.message
                )
            }
        }
    }

    fun cambiaSezione(sezione: SezionePreferiti) {
        _state.update { it.copy(sezione = sezione, errore = null) }
    }

    // --- dettaglio --------------------------------------------------------------------

    /** Apre il dettaglio: gli itinerari non stanno negli elenchi, vanno chiesti al backend. */
    fun apriLista(listaId: Long) {
        _state.update { it.copy(operazioneInCorso = true, errore = null) }
        viewModelScope.launch {
            val risultato = repository.getLista(listaId)
            _state.update { stato ->
                stato.copy(
                    operazioneInCorso = false,
                    listaAperta = risultato.getOrNull() ?: stato.listaAperta,
                    errore = risultato.exceptionOrNull()?.message
                )
            }
        }
    }

    fun chiudiLista() {
        _state.update { it.copy(listaAperta = null, errore = null) }
    }

    // --- ciclo di vita delle liste ----------------------------------------------------

    fun creaLista(nome: String, visibilita: VisibilitaLista) {
        if (nome.isBlank()) {
            _state.update { it.copy(errore = "Dai un nome alla lista") }
            return
        }
        esegui("Lista creata") { repository.creaLista(nome.trim(), visibilita).map { } }
    }

    /**
     * Passa da privata a condivisa e viceversa.
     *
     * Tornare privata revoca ogni accesso: e' un'operazione che il backend esegue in un
     * colpo solo, qui basta ricaricare per vedere i destinatari spariti.
     */
    fun cambiaVisibilita(lista: ListaPreferiti, visibilita: VisibilitaLista) {
        val messaggio = if (visibilita == VisibilitaLista.PRIVATA) {
            "Lista tornata privata: le condivisioni sono state revocate"
        } else {
            "Lista condivisibile: ora puoi scegliere con chi"
        }
        esegui(messaggio, lista.id) {
            repository.aggiornaLista(lista.id, lista.nome, visibilita).map { }
        }
    }

    fun eliminaLista(listaId: Long) {
        _state.update { it.copy(listaAperta = null) }
        esegui("Lista eliminata") { repository.eliminaLista(listaId) }
    }

    // --- contenuto e condivisioni -----------------------------------------------------

    fun rimuoviItinerario(listaId: Long, itinerarioId: Long) {
        esegui("Itinerario rimosso", listaId) {
            repository.rimuoviItinerario(listaId, itinerarioId).map { }
        }
    }

    fun condividiConEmail(listaId: Long, email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(errore = "Indica l'email della persona con cui condividere") }
            return
        }
        esegui("Lista condivisa", listaId) {
            repository.condividiConEmail(listaId, email.trim()).map { }
        }
    }

    fun revocaCondivisione(listaId: Long, utenteId: Long) {
        esegui("Accesso revocato", listaId) {
            repository.revocaCondivisione(listaId, utenteId).map { }
        }
    }

    fun messaggioMostrato() {
        _state.update { it.copy(messaggio = null, errore = null) }
    }

    // --- supporto ---------------------------------------------------------------------

    /**
     * Esegue un'operazione di scrittura e riallinea lo stato al backend.
     *
     * Il ricalcolo passa sempre da una nuova lettura invece di aggiornare la copia locale:
     * una modifica alla visibilita' cambia anche cosa vedono le altre sezioni, e ricostruire
     * a mano quelle conseguenze e' il modo piu' rapido per farle divergere.
     */
    private fun esegui(
        messaggio: String,
        listaDaRicaricare: Long? = null,
        operazione: suspend () -> Result<Unit>
    ) {
        _state.update { it.copy(operazioneInCorso = true, errore = null, messaggio = null) }
        viewModelScope.launch {
            val risultato = operazione()

            if (risultato.isFailure) {
                _state.update {
                    it.copy(operazioneInCorso = false, errore = risultato.exceptionOrNull()?.message)
                }
                return@launch
            }

            val mie = repository.getMieListe()
            val condivise = repository.getListeCondiviseConMe()
            val dettaglio = listaDaRicaricare?.let { repository.getLista(it).getOrNull() }

            _state.update { stato ->
                stato.copy(
                    operazioneInCorso = false,
                    mieListe = mie.getOrDefault(stato.mieListe),
                    condiviseConMe = condivise.getOrDefault(stato.condiviseConMe),
                    listaAperta = if (listaDaRicaricare == null) stato.listaAperta else dettaglio,
                    messaggio = messaggio,
                    errore = null
                )
            }
        }
    }
}
