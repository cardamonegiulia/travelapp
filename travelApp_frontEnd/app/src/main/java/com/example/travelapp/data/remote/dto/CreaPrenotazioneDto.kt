package com.example.travelapp.data.remote.dto

data class CreaPrenotazioneDto(
    val disponibilitaItinerarioId: Long? = null,
    val sessioneSingolaAttivitaId: Long? = null,
    val numeroPartecipanti: Int,
    val attivitaExtraIds: List<Long>? = null
)