package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.Recensione

/**
 * Recensione cosi' come la restituisce il backend.
 *
 * L'autore arriva gia' come nome e cognome: l'app non ha (e non deve avere) modo di
 * risalire all'anagrafica completa di chi ha scritto la recensione.
 */
data class RecensioneResponseDto(
    val id: Long,
    val prenotazioneId: Long?,
    val itinerarioId: Long?,
    val itinerarioTitolo: String?,
    val utenteId: Long?,
    val votazione: Int,
    val comm: String?,
    val autoreNome: String?,
    val autoreCognome: String?,
    val dataRecensione: String?
) {
    fun toDomain(): Recensione = Recensione(
        id = id,
        prenotazioneId = prenotazioneId,
        itinerarioId = itinerarioId,
        titoloItinerario = itinerarioTitolo?.takeIf { it.isNotBlank() },
        autoreId = utenteId,
        votazione = votazione,
        commento = comm?.takeIf { it.isNotBlank() },
        autore = listOfNotNull(autoreNome, autoreCognome)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Viaggiatore" },
        data = dataRecensione
    )
}

/** Creazione: si recensisce una prenotazione conclusa, il commento e' facoltativo. */
data class CreaRecensioneDto(
    val prenotazioneId: Long,
    val votazione: Int,
    val comm: String?
)

/** Modifica della propria recensione: cambiano solo stelle e commento. */
data class AggiornaRecensioneDto(
    val votazione: Int,
    val comm: String?
)
