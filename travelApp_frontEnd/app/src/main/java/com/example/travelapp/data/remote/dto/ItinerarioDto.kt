package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.Itinerario
import java.math.BigDecimal

data class ItinerarioResponseDto(
    val id: Long,
    val organizzatoreId: Long?,
    val titolo: String,
    val descrizione: String?,
    val destinazionePrincipale: String?,
    val prezzoBase: BigDecimal?,
    val durataGiorni: Int?,
    val maxPartecipanti: Int?,
    val stato: String?
) {
    fun toDomain(): Itinerario = Itinerario(
        id = id,
        organizzatoreId = organizzatoreId,
        titolo = titolo,
        descrizione = descrizione,
        destinazionePrincipale = destinazionePrincipale,
        prezzoBase = prezzoBase,
        durataGiorni = durataGiorni,
        maxPartecipanti = maxPartecipanti,
        stato = stato
    )
}

data class ItinerarioRequestDto(
    val titolo: String,
    val descrizione: String?,
    val destinazionePrincipale: String,
    val prezzoBase: BigDecimal,
    val durataGiorni: Int,
    val maxPartecipanti: Int
)

data class PageResponse<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val number: Int
)