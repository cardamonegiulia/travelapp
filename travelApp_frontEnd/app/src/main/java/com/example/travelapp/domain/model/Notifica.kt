package com.example.travelapp.domain.model

enum class TipoNotifica {
    INVITO_RECENSIONE,

    SCONOSCIUTO
}

data class Notifica(
    val id: Long,
    val tipo: TipoNotifica,
    val titolo: String,
    val messaggio: String,
    val letta: Boolean,
    val data: String?,
    val prenotazioneId: Long?,
    val itinerarioId: Long?,
    val titoloViaggio: String?
)
