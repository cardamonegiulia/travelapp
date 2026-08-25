package com.example.travelapp.ui.prenotazioni

import com.example.travelapp.domain.model.Pagamento
import com.example.travelapp.domain.model.Prenotazione

data class BookingUiState(
    val numeroPartecipanti: Int = 1,

    val extraSelezionati: Map<Long, Double> = emptyMap(),

    val disponibilitaItinerarioId: Long? = null,
    val sessioneSingolaAttivitaId: Long? = null,

    val titolo: String = "",
    val luogo: String = "",
    val prezzoBaseUnitario: Double = 0.0,
    val prezzoBase: Double = 0.0,

    val prezzoExtra: Double = 0.0,
    val prezzoTotaleVisualizzato: Double = 0.0,
    val metodoPagamento: MetodoPagamentoUi = MetodoPagamentoUi.CARTA_CREDITO,

    val isLoading: Boolean = false,
    val errore: String? = null,

    val prenotazioneCreata: Prenotazione? = null,
    val pagamentoCompletato: Pagamento? = null
) {
    val attivitaExtraIds: List<Long>
        get() = extraSelezionati.keys.toList()
}