package com.example.travelapp.data.remote.dto

data class DisponibilitaItinerarioResponseDto(
    val id: Long,
    val dataInizio: String,
    val dataFine: String,
    val postiDisponibili: Int,
    val stato: String? = null
)

data class SessioneAttivitaResponseDto(
    val id: Long,
    val dataInizio: String,
    val dataFine: String,
    val postiDisponibili: Int,
    val stato: String? = null
)