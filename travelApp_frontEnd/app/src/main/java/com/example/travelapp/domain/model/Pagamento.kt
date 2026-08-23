package com.example.travelapp.domain.model

data class Pagamento(
    val id: Long,
    val prenotazioneId: Long,
    val importo: Double,
    val statoPagamento: StatoPagamento,
    val statoPrenotazione: StatoPrenotazione,
    val dataPagamento: String?
)