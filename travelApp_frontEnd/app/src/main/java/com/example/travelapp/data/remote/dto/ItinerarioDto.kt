package com.example.travelapp.data.remote.dto

import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.domain.model.GiornoProgramma
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
    val dataInizio: String? = null,
    val dataFine: String? = null,
    val dataLimitePrenotazione: String? = null,
    val maxPartecipanti: Int?,
    val stato: String?,
    val mediaVoti: Double? = null,
    val numeroRecensioni: Long = 0,
    val dateDisponibili: Boolean = false,
    val programma: List<GiornoProgrammaDto>? = null,
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
        dataInizio = dataInizio,
        dataFine = dataFine,
        dataLimitePrenotazione = dataLimitePrenotazione,
        maxPartecipanti = maxPartecipanti,
        stato = stato,
        mediaVoti = mediaVoti,
        numeroRecensioni = numeroRecensioni,
        dateDisponibili = dateDisponibili,
        programma = programma
            ?.mapIndexed { indice, giorno ->
                GiornoProgramma(
                    giorno = giorno.giorno ?: (indice + 1),
                    titolo = giorno.titolo,
                    descrizione = giorno.descrizione
                )
            }
            ?: emptyList(),
        immagini = immagini?.map { img ->
            ImmagineResponse(
                id = img.id,
                url = ApiClient.urlAssoluto(img.url),
                tipo = img.contentType
            )
        } ?: emptyList()
    )
}

data class GiornoProgrammaDto(
    val giorno: Int? = null,
    val titolo: String,
    val descrizione: String
)

data class ItinerarioRequestDto(
    val titolo: String,
    val descrizione: String?,
    val destinazionePrincipale: String,
    val prezzoBase: BigDecimal,
    val dataInizio: String? = null,
    val dataFine: String? = null,
    val dataLimitePrenotazione: String? = null,
    val durataGiorni: Int? = null,
    val maxPartecipanti: Int,
    val programma: List<GiornoProgrammaDto>
)

data class PageResponse<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val number: Int
)