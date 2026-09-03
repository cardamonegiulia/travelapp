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
    val dataInizio: String? = null,
    val dataFine: String? = null,
    val dataLimitePrenotazione: String? = null,
    val maxPartecipanti: Int?,
    val stato: String?,

    val mediaVoti: Double? = null,

    val numeroRecensioni: Long = 0,

    val dateDisponibili: Boolean = false,

    val programma: List<GiornoProgramma> = emptyList(),

    val immagini: List<ImmagineResponse> = emptyList()
)