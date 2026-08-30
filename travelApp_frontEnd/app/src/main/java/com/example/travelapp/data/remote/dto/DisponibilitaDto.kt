package com.example.travelapp.data.remote.dto

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DisponibilitaItinerarioResponseDto(
    val id: Long,
    val dataInizio: String,
    val dataFine: String,
    // Ultimo giorno utile per prenotare: assente se si puo' prenotare fino alla partenza.
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

// Le date del backend arrivano in ISO ("2026-09-09" oppure "2026-09-09T00:00:00"): teniamo
// solo il giorno e confrontiamo le stringhe, che in ISO si ordinano gia' cronologicamente.
// Cosi' evitiamo java.time, non disponibile su minSdk 24 senza desugaring.
fun giornoIso(isoDateTime: String?): String? =
    isoDateTime?.substringBefore('T')?.takeIf { it.isNotBlank() }

fun oggiIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/** Data leggibile per l'utente (gg/MM/aaaa) a partire da una data ISO del backend. */
fun dataLeggibile(isoDateTime: String?): String? =
    giornoIso(isoDateTime)
        ?.split("-")
        ?.takeIf { it.size == 3 }
        ?.let { "${it[2]}/${it[1]}/${it[0]}" }

/**
 * Si prenota fino al termine fissato dall'organizzatore o, se non ne ha fissato uno, fino
 * alla partenza: e' la stessa regola che applica il server.
 */
fun DisponibilitaItinerarioResponseDto.prenotazioniAperte(oggi: String = oggiIso()): Boolean {
    val termine = giornoIso(dataLimitePrenotazione) ?: giornoIso(dataInizio)
    return termine == null || termine >= oggi
}

fun DisponibilitaItinerarioResponseDto.isPrenotabile(oggi: String = oggiIso()): Boolean =
    postiDisponibili > 0 && prenotazioniAperte(oggi)
