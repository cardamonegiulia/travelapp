package com.example.travelapp.domain.model

data class Recensione(
    val id: Long,
    val prenotazioneId: Long?,
    val itinerarioId: Long?,
    val titoloItinerario: String? = null,
    val autoreId: Long?,
    val votazione: Int,
    val commento: String?,
    val autore: String,
    val data: String?
)
