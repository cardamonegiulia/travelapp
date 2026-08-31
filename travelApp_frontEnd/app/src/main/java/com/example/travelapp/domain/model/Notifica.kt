package com.example.travelapp.domain.model

enum class TipoNotifica {
    /** Invito a recensire un viaggio appena concluso. */
    INVITO_RECENSIONE,

    /** Tipo non ancora conosciuto da questa versione dell'app: si mostra come notifica semplice. */
    SCONOSCIUTO
}

/**
 * Notifica in-app.
 *
 * [prenotazioneId] e [itinerarioId] sono i riferimenti su cui costruire l'azione diretta:
 * per l'invito a recensire, aprire il form della recensione di quel viaggio.
 */
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
