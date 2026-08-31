package com.example.travelapp.data.remote.dto

import java.math.BigDecimal

data class AttivitaExtraResponseDto(
    val id: Long,
    val titolo: String,
    val prezzoExtra: BigDecimal
)