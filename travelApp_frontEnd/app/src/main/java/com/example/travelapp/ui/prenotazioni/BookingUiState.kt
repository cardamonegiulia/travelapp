package com.example.travelapp.ui.prenotazioni

import com.example.travelapp.domain.model.Pagamento
import com.example.travelapp.domain.model.Prenotazione

data class BookingUiState(
    val numeroPartecipanti: Int = 1,
    val attivitaExtraIds: List<Long> = emptyList(),

    // Dati dell'elemento che l'utente sta prenotando
    val titolo: String = "",
    val luogo: String = "",
    val prezzoBase: Double = 0.0,

    // Prezzi mostrati nel riepilogo del wizard.
    // Il prezzo definitivo viene comunque calcolato dal backend.
    val prezzoExtra: Double = 0.0,
    val prezzoTotaleVisualizzato: Double = 0.0,

    val isLoading: Boolean = false,
    val errore: String? = null,

    val prenotazioneCreata: Prenotazione? = null,
    val pagamentoCompletato: Pagamento? = null
)