package com.example.travelapp.ui.preferiti

import com.example.travelapp.domain.model.ListaPreferiti


enum class SezionePreferiti(val etichetta: String) {
    MIE("Le mie liste"),
    CONDIVISE_CON_ME("Condivise con me")
}


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
