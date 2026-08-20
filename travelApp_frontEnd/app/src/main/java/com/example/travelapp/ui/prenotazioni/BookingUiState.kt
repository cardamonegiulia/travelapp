package com.example.travelapp.ui.prenotazioni

import com.example.travelapp.domain.model.Pagamento
import com.example.travelapp.domain.model.Prenotazione

data class BookingUiState(
    val numeroPartecipanti: Int = 1,
    val attivitaExtraIds: List<Long> = emptyList(),
    val isLoading: Boolean = false,
    val errore: String? = null,
    val prenotazioneCreata: Prenotazione? = null,
    val pagamentoCompletato: Pagamento? = null
)