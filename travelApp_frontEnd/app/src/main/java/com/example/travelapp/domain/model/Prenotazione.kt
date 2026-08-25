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
    val dataPrenotazione: String
)



