package com.example.travelapp.ui.preferiti

import com.example.travelapp.domain.model.ListaPreferiti

/** Le due sezioni della schermata: le liste dell'utente e quelle ricevute in condivisione. */
enum class SezionePreferiti(val etichetta: String) {
    MIE("Le mie liste"),
    CONDIVISE_CON_ME("Condivise con me")
}

/**
 * Stato della schermata "Preferiti".
 *
 * [listaAperta] e' il dettaglio: quando e' valorizzata la schermata mostra gli itinerari
 * di quella lista invece dell'elenco. E' un campo dello stato e non una destinazione di
 * navigazione perche' il dettaglio va ricaricato dal backend a ogni modifica, e tenerlo
 * qui evita di dover propagare l'id avanti e indietro.
 */
data class PreferitiUiState(
    val isLoading: Boolean = true,
    val sezione: SezionePreferiti = SezionePreferiti.MIE,
    val mieListe: List<ListaPreferiti> = emptyList(),
    val condiviseConMe: List<ListaPreferiti> = emptyList(),
    val listaAperta: ListaPreferiti? = null,
    val operazioneInCorso: Boolean = false,
    val messaggio: String? = null,
    val errore: String? = null
) {
    val listeVisibili: List<ListaPreferiti>
        get() = when (sezione) {
            SezionePreferiti.MIE -> mieListe
            SezionePreferiti.CONDIVISE_CON_ME -> condiviseConMe
        }
}
