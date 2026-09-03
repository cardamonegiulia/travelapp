package com.example.travelapp.domain.model

data class Prenotazione(
    val id: Long,
    val titolo: String,
    val luogo: String,
    val numeroPartecipanti: Int,
    val prezzoTotale: Double,
    val statoPrenotazione: StatoPrenotazione,
    val statoPagamento: StatoPagamento?,
    val tipoPrenotazione: TipoPrenotazione,
    val dataPrenotazione: String,
    val dataInizioViaggio: String? = null,
    val dataFineViaggio: String? = null,
    val itinerarioId: Long? = null,
    val conclusa: Boolean = false,
    val recensibile: Boolean = false,
    val recensioneId: Long? = null,
    val secondiRimanentiPagamento: Long = 0L,
) {
    val dataInizio: String?
        get() = dataInizioViaggio
    val dataFine: String?
        get() = dataFineViaggio
}