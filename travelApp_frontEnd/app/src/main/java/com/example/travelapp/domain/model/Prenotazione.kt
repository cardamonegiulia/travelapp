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

    // Periodo del viaggio prenotato, quando presente.
    val dataInizioViaggio: String? = null,
    val dataFineViaggio: String? = null,

    // Presente solo per prenotazioni relative a un itinerario.
    val itinerarioId: Long? = null,

    // Informazioni relative alla conclusione del viaggio e alle recensioni.
    val conclusa: Boolean = false,
    val recensibile: Boolean = false,
    val recensioneId: Long? = null
) {

    /*
     * Alias mantenuti per compatibilità con il booking
     * che utilizza ancora dataInizio / dataFine.
     */
    val dataInizio: String?
        get() = dataInizioViaggio

    val dataFine: String?
        get() = dataFineViaggio
}