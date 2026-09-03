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

data class DestinatarioCondivisione(
    val id: Long,
    val nome: String,
    val cognome: String,
    val email: String
) {
    val nomeCompleto: String get() = "$nome $cognome".trim()
}

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
