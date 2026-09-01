package com.example.travelapp.domain.model

/**
 * Una recensione lasciata su un viaggio concluso.
 *
 * [commento] e' nullo quando il viaggiatore ha lasciato solo le stelle: il commento e'
 * facoltativo, la valutazione no.
 */
data class Recensione(
    val id: Long,
    val prenotazioneId: Long?,
    val itinerarioId: Long?,
    /** Titolo del viaggio recensito: nullo se il backend non lo manda (recensione orfana). */
    val titoloItinerario: String? = null,
    val autoreId: Long?,
    val votazione: Int,
    val commento: String?,
    val autore: String,
    val data: String?
)
