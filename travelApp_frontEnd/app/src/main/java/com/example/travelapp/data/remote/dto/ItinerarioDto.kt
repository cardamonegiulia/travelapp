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
    // Valutazione media: null quando l'itinerario non ha ancora recensioni.
    val mediaVoti: Double? = null,
    val numeroRecensioni: Long = 0,
    // false quando non resta nessuna partenza prenotabile: l'itinerario resta comunque
    // in bacheca, con un'etichetta che lo dice.
    val dateDisponibili: Boolean = false,
    // Programma giorno per giorno. Assente sugli itinerari creati prima che diventasse
    // obbligatorio: la scheda in quel caso lo dice, invece di mostrare una sezione vuota.
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
                    // Il progressivo lo assegna il server; se manca vale la posizione.
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

/**
 * Una giornata del programma. Serve sia in risposta sia in richiesta: in richiesta il
 * campo [giorno] resta null, perche' la numerazione la assegna il server dalla posizione.
 */
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
    // Periodo di una partenza (ISO yyyy-MM-dd): il server ne ricava la durata in giorni.
    // In aggiornamento e' il periodo di una partenza NUOVA, non una correzione di quelle
    // gia' pubblicate; se non se ne aggiunge nessuna resta null e si manda la durata.
    val dataInizio: String? = null,
    val dataFine: String? = null,
    // Termine per le prenotazioni: null significa "fino alla partenza".
    val dataLimitePrenotazione: String? = null,
    // Serve quando non si invia un periodo: il server esige di poter determinare la durata.
    val durataGiorni: Int? = null,
    val maxPartecipanti: Int,
    // Obbligatorio come il titolo o il prezzo: almeno una giornata, in ordine.
    val programma: List<GiornoProgrammaDto>
)

data class PageResponse<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val number: Int
)