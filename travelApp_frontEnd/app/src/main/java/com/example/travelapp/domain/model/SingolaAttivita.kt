package com.example.travelapp.domain.model

import java.math.BigDecimal

data class SingolaAttivita(
    val id: Long,
    val organizzatoreId: Long?,
    val titolo: String,
    val descrizione: String?,
    val luogo: String?,
    val prezzo: BigDecimal?,
    val durataMinuti: Int?,
    val maxPartecipanti: Int?
)