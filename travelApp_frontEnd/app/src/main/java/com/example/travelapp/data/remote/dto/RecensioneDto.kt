package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.Recensione

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


data class CreaRecensioneDto(
    val prenotazioneId: Long,
    val votazione: Int,
    val comm: String?
)
data class AggiornaRecensioneDto(
    val votazione: Int,
    val comm: String?
)
