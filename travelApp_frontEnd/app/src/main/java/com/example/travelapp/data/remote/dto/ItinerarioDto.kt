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
    val dataInizio: String? = null,
    val dataFine: String? = null,
    val dataLimitePrenotazione: String? = null,
    val maxPartecipanti: Int?,
    val stato: String?,
    // Valutazione media: null quando l'itinerario non ha ancora recensioni.
    val mediaVoti: Double? = null,
    val numeroRecensioni: Long = 0,
    // false quando non resta nessuna partenza prenotabile: l'itinerario resta comunque
    // in bacheca, con un'etichetta che lo dice.
    val dateDisponibili: Boolean = false,
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
    // Il periodo del viaggio (formato ISO yyyy-MM-dd): il server ne ricava la durata in giorni.
    val dataInizio: String,
    val dataFine: String,
    // Termine per le prenotazioni: null significa "fino alla partenza".
    val dataLimitePrenotazione: String?,
    val maxPartecipanti: Int
)

data class PageResponse<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val number: Int
)