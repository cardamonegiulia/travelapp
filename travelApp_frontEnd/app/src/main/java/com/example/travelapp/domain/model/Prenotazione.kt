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

    // Periodo del viaggio prenotato, quando c'e' (formato ISO del backend).
    val dataInizioViaggio: String? = null,
    val dataFineViaggio: String? = null,

    /** Itinerario prenotato: assente per le prenotazioni di una singola attivita'. */
    val itinerarioId: Long? = null,

    /** true quando il viaggio e' finito e la prenotazione non e' stata cancellata. */
    val conclusa: Boolean = false,

    /** true quando si puo' lasciare una recensione adesso: deciso dal server. */
    val recensibile: Boolean = false,

    /** Recensione gia' scritta su questo viaggio, se c'e': serve ad aprirla in modifica. */
    val recensioneId: Long? = null
)



