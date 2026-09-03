package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.Notifica
import com.example.travelapp.domain.model.TipoNotifica

data class NotificaDto(
    val id: Long,
    val tipo: String?,
    val titolo: String?,
    val messaggio: String?,
    val letta: Boolean = false,
    val dataCreazione: String?,
    val prenotazioneId: Long?,
    val itinerarioId: Long?,
    val titoloViaggio: String?
) {
    fun toDomain(): Notifica = Notifica(
        id = id,
        tipo = when (tipo) {
            "INVITO_RECENSIONE" -> TipoNotifica.INVITO_RECENSIONE
            else -> TipoNotifica.SCONOSCIUTO
        },
        titolo = titolo.orEmpty().ifBlank { "Notifica" },
        messaggio = messaggio.orEmpty(),
        letta = letta,
        data = dataCreazione,
        prenotazioneId = prenotazioneId,
        itinerarioId = itinerarioId,
        titoloViaggio = titoloViaggio
    )
}
