package com.example.travelapp.ui.prenotazioni

import com.example.travelapp.domain.model.Pagamento
import com.example.travelapp.domain.model.Prenotazione

data class BookingUiState(
    val numeroPartecipanti: Int = 1,

    val extraSelezionati: Map<Long, Double> = emptyMap(),
    val extraDisponibili: List<ExtraUi> = emptyList(),
    val extraInCaricamento: Boolean = false,

    val itinerarioId: Long? = null,
    val disponibilitaItinerarioId: Long? = null,
    val sessioneSingolaAttivitaId: Long? = null,

    val titolo: String = "",
    val luogo: String = "",

    val dataInizio: String? = null,
    val dataFine: String? = null,
    val postiDisponibili: Int? = null,

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

    val puoIncrementarePartecipanti: Boolean
        get() =
            postiDisponibili == null ||
                    numeroPartecipanti < postiDisponibili
}