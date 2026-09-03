package com.example.travelapp.data.remote.dto

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DisponibilitaItinerarioResponseDto(
    val id: Long,
    val dataInizio: String,
    val dataFine: String,
    val dataLimitePrenotazione: String? = null,
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
fun giornoIso(isoDateTime: String?): String? =
    isoDateTime?.substringBefore('T')?.takeIf { it.isNotBlank() }

fun oggiIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun dataLeggibile(isoDateTime: String?): String? =
    giornoIso(isoDateTime)
        ?.split("-")
        ?.takeIf { it.size == 3 }
        ?.let { "${it[2]}/${it[1]}/${it[0]}" }

fun DisponibilitaItinerarioResponseDto.prenotazioniAperte(oggi: String = oggiIso()): Boolean {
    val termine = giornoIso(dataLimitePrenotazione) ?: giornoIso(dataInizio)
    return termine == null || termine >= oggi
}

fun DisponibilitaItinerarioResponseDto.isPrenotabile(oggi: String = oggiIso()): Boolean =
    postiDisponibili > 0 && prenotazioniAperte(oggi)
