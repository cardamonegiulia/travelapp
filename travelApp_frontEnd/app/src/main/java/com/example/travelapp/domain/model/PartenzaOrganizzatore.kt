package com.example.travelapp.domain.model

data class PartenzaOrganizzatore(
    val disponibilitaId: Long,
    val dataInizio: String?,
    val dataFine: String?,
    val postiDisponibili: Int?,
    val numeroPrenotazioni: Long,
    val partecipantiTotali: Long
) {
    val senzaPrenotazioni: Boolean
        get() = numeroPrenotazioni == 0L
}

data class PrenotatoPartenza(
    val prenotazioneId: Long,
    val nome: String,
    val cognome: String,
    val numeroPartecipanti: Int,
    val prezzoTotale: Double,
    val statoPrenotazione: StatoPrenotazione,
    val statoPagamento: StatoPagamento?,
    val dataPrenotazione: String
) {
    val nomeCompleto: String
        get() = "$nome $cognome".trim()
}
