package com.example.travelapp.data.remote.dto

import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.domain.model.Utente

/**
 * Risposta JSON del profilo utente (`UtenteResponseDto` lato backend).
 *
 * Il `keycloakId` non c'è di proposito: il backend non lo espone mai.
 */
data class UtenteResponseDto(
    val id: Long,
    val nome: String?,
    val cognome: String?,
    val email: String?,
    val ruolo: String?,
    val tema: String?,
    val fotoProfilo: ImmagineDto?
) {
    fun toDomain(): Utente = Utente(
        id = id,
        nome = nome.orEmpty(),
        cognome = cognome.orEmpty(),
        email = email.orEmpty(),
        ruolo = ruolo,
        tema = tema,
        // il backend restituisce un percorso relativo ("/api/immagini/12/contenuto"):
        // alla UI serve l'url completo da cui scaricare i byte
        fotoProfiloUrl = fotoProfilo?.url?.let { ApiClient.urlAssoluto(it) }
    )
}

/** Metadati di un'immagine (`ImmagineResponse` lato backend). */
data class ImmagineDto(
    val id: Long,
    val url: String,
    val contentType: String?,
    val dimensioneByte: Long?,
    val larghezza: Int?,
    val altezza: Int?,
    val proprietarioId: Long?
)
