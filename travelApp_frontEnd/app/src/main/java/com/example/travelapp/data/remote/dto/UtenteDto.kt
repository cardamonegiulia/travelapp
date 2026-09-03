package com.example.travelapp.data.remote.dto

import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.domain.model.Utente
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
        fotoProfiloUrl = fotoProfilo?.url?.let { ApiClient.urlAssoluto(it) }
    )
}

data class AggiornaTemaDto(
    val tema: String
)
data class CambioPasswordDto(
    val nuovaPassword: String
)
data class ImmagineDto(
    val id: Long,
    val url: String,
    val contentType: String?,
    val dimensioneByte: Long?,
    val larghezza: Int?,
    val altezza: Int?,
    val proprietarioId: Long?
)
