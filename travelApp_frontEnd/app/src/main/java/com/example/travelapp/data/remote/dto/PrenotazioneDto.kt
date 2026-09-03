package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.domain.model.StatoPagamento
import com.example.travelapp.domain.model.StatoPrenotazione
import com.example.travelapp.domain.model.TipoPrenotazione

data class PrenotazioneDto(
    val id: Long,
    val viaggiatoreId: Long,
    val nomeViaggiatore: String,
    val cognomeViaggiatore: String,
    val disponibilitaItinerarioId: Long?,
    val sessioneSingolaAttivitaId: Long?,
    val destinazione: String?,
    val numeroPartecipanti: Int,
    val prezzoTotale: Double,
    val statoPrenotazione: StatoPrenotazione,
    val statoPagamento: StatoPagamento?,
    val dataPrenotazione: String,
    val tipoPrenotazione: TipoPrenotazione,
    val titolo: String,
    val luogo: String,

    val dataInizioViaggio: String? = null,
    val dataFineViaggio: String? = null,

    val itinerarioId: Long? = null,

    val conclusa: Boolean = false,
    val recensibile: Boolean = false,
    val recensioneId: Long? = null,

    val secondiRimanentiPagamento: Long = 0L,
)

fun PrenotazioneDto.toDomain(): Prenotazione {
    return Prenotazione(
        id = id,
        titolo = titolo,
        luogo = luogo,
        numeroPartecipanti = numeroPartecipanti,
        prezzoTotale = prezzoTotale,
        statoPrenotazione = statoPrenotazione,
        statoPagamento = statoPagamento,
        tipoPrenotazione = tipoPrenotazione,
        dataPrenotazione = dataPrenotazione,

        dataInizioViaggio = dataInizioViaggio,
        dataFineViaggio = dataFineViaggio,

        itinerarioId = itinerarioId,
        conclusa = conclusa,
        recensibile = recensibile,
        recensioneId = recensioneId,

        secondiRimanentiPagamento = secondiRimanentiPagamento
    )
}