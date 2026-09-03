package com.example.travelapp.domain.model


enum class VisibilitaLista {
    PRIVATA,
    CONDIVISA,
    SCONOSCIUTA;

    companion object {
        fun da(valore: String?): VisibilitaLista = when (valore?.uppercase()) {
            "PRIVATA" -> PRIVATA
            "CONDIVISA" -> CONDIVISA
            else -> SCONOSCIUTA
        }
    }
}

/** Utente con cui una lista e' condivisa. */
data class DestinatarioCondivisione(
    val id: Long,
    val nome: String,
    val cognome: String,
    val email: String
) {
    val nomeCompleto: String get() = "$nome $cognome".trim()
}

/**
 * Una lista di itinerari preferiti.
 *
 * [itinerari] e' popolata solo quando si apre il dettaglio: negli elenchi il backend manda
 * il solo [numeroItinerari]. [proprietaria] distingue le liste dell'utente da quelle che
 * qualcun altro ha condiviso con lui, che sono in sola lettura.
 */
data class ListaPreferiti(
    val id: Long,
    val nome: String,
    val visibilita: VisibilitaLista,
    val proprietarioId: Long?,
    val proprietarioNome: String?,
    val proprietaria: Boolean,
    val numeroItinerari: Int,
    val itinerari: List<Itinerario> = emptyList(),
    val destinatari: List<DestinatarioCondivisione> = emptyList()
) {
    val eCondivisa: Boolean get() = visibilita == VisibilitaLista.CONDIVISA
}
