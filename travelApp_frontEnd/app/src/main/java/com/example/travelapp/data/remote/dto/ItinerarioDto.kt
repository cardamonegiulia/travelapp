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
    // Nullable di proposito: Gson costruisce l'oggetto senza passare dal
    // costruttore Kotlin, quindi un default non-null non lo proteggerebbe da una
    // risposta priva del campo: il valore resterebbe null e schianterebbe qui
    // sotto. Il backend lo manda sempre, ma non e' detto sia aggiornato.
    val immagini: List<ImmagineResponse>? = null
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
        // Il backend restituisce un percorso relativo (/api/immagini/1/contenuto):
        // a Coil serve un URL assoluto, come gia' si fa per la foto profilo.
        immagini = immagini.orEmpty().map { immagine ->
            immagine.copy(url = immagine.url?.let { ApiClient.urlAssoluto(it) })
        }
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