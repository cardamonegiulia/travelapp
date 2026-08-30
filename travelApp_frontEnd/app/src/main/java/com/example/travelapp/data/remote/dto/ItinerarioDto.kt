package com.example.travelapp.data.remote.dto

import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.domain.model.ImmagineResponse
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
    val stato: String?,
    val immagini: List<ImmagineDto>? = null
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
        stato = stato,
        immagini = immagini?.map { img ->
            ImmagineResponse(
                id = img.id,
                url = ApiClient.urlAssoluto(img.url),
                tipo = img.contentType
            )
        } ?: emptyList()
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