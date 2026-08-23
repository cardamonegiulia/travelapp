package com.example.travelapp.domain.model

import java.math.BigDecimal

data class Itinerario(
    val id: Long,
    val organizzatoreId: Long?,
    val titolo: String,
    val descrizione: String?,
    val destinazionePrincipale: String?,
    val prezzoBase: BigDecimal?,
    val durataGiorni: Int?,
    val maxPartecipanti: Int?,
    val stato: String?,
    val immagini: List<ImmagineResponse> = emptyList()
)