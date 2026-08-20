package com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.SingolaAttivita
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class SingolaAttivitaResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("organizzatoreId") val organizzatoreId: Long?,
    @SerializedName("titolo") val titolo: String,
    @SerializedName("descrizione") val descrizione: String?,
    @SerializedName("luogo") val luogo: String?,
    @SerializedName("prezzo") val prezzo: BigDecimal?,
    @SerializedName("durataMinuti") val durataMinuti: Int?,
    @SerializedName("maxPartecipanti") val maxPartecipanti: Int?
) {
    fun toDomain(): SingolaAttivita = SingolaAttivita(
        id = id,
        organizzatoreId = organizzatoreId,
        titolo = titolo,
        descrizione = descrizione,
        luogo = luogo,
        prezzo = prezzo,
        durataMinuti = durataMinuti,
        maxPartecipanti = maxPartecipanti
    )
}

data class SingolaAttivitaRequestDto(
    @SerializedName("titolo") val titolo: String,
    @SerializedName("descrizione") val descrizione: String?,
    @SerializedName("luogo") val luogo: String,
    @SerializedName("prezzo") val prezzo: BigDecimal,
    @SerializedName("durataMinuti") val durataMinuti: Int,
    @SerializedName("maxPartecipanti") val maxPartecipanti: Int
)