package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.DestinatarioCondivisione
import com.example.travelapp.domain.model.ListaPreferiti
import com.example.travelapp.domain.model.VisibilitaLista

/**
 * Lista di preferiti come la restituisce `/api/preferiti`.
 *
 * `visibilita` arriva come stringa e non come enum: Gson, davanti a un valore che non
 * conosce, non solleva un errore ma lascia il campo a null, e in Kotlin un enum non
 * nullable dichiarato tale diventerebbe null di nascosto. Meglio convertirlo a mano.
 */
data class ListaPreferitiResponseDto(
    val id: Long,
    val nome: String?,
    val visibilita: String?,
    val proprietarioId: Long?,
    val proprietarioNome: String?,
    val proprietaria: Boolean = false,
    val numeroItinerari: Int = 0,
    val itinerari: List<ItinerarioResponseDto>? = null,
    val destinatari: List<DestinatarioCondivisioneDto>? = null
) {
    fun toDomain(): ListaPreferiti = ListaPreferiti(
        id = id,
        nome = nome.orEmpty().ifBlank { "Lista senza nome" },
        visibilita = VisibilitaLista.da(visibilita),
        proprietarioId = proprietarioId,
        proprietarioNome = proprietarioNome,
        proprietaria = proprietaria,
        numeroItinerari = numeroItinerari,
        itinerari = itinerari.orEmpty().map { it.toDomain() },
        destinatari = destinatari.orEmpty().map { it.toDomain() }
    )
}

data class DestinatarioCondivisioneDto(
    val id: Long,
    val nome: String?,
    val cognome: String?,
    val email: String?
) {
    fun toDomain(): DestinatarioCondivisione = DestinatarioCondivisione(
        id = id,
        nome = nome.orEmpty(),
        cognome = cognome.orEmpty(),
        email = email.orEmpty()
    )
}

/** Creazione e modifica di una lista: nome e visibilita', nient'altro. */
data class ListaPreferitiRequestDto(
    val nome: String,
    val visibilita: String
)

/** Corpo di `POST .../itinerari`. */
data class PreferitoItinerarioRequestDto(
    val itinerarioId: Long
)

/**
 * Corpo di `POST .../condivisioni`: si indica l'utente per email - quella che si conosce
 * di un amico - oppure per id. Il campo non valorizzato resta null e non viene serializzato.
 */
data class CondivisioneRequestDto(
    val email: String? = null,
    val utenteId: Long? = null
)
