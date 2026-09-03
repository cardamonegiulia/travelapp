package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.DestinatarioCondivisione
import com.example.travelapp.domain.model.ListaPreferiti
import com.example.travelapp.domain.model.VisibilitaLista

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

data class ListaPreferitiRequestDto(
    val nome: String,
    val visibilita: String
)

data class PreferitoItinerarioRequestDto(
    val itinerarioId: Long
)
data class CondivisioneRequestDto(
    val email: String? = null,
    val utenteId: Long? = null
)
