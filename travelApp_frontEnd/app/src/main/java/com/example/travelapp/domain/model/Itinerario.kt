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

    /** Media delle stelle, null se nessuno ha ancora recensito questo itinerario. */
    val mediaVoti: Double? = null,

    /** Su quante recensioni e' calcolata [mediaVoti]. */
    val numeroRecensioni: Long = 0,

    /**
     * true se resta almeno una partenza prenotabile.
     *
     * Un itinerario senza date NON sparisce dalla bacheca: l'organizzatore puo' aggiungerne
     * di nuove quando vuole, quindi resta visibile con un'etichetta esplicita.
     */
    val dateDisponibili: Boolean = false,

    /**
     * Programma giorno per giorno scritto dall'organizzatore, gia' ordinato.
     *
     * Vuoto solo per gli itinerari pubblicati prima che il programma diventasse
     * obbligatorio: la scheda lo segnala esplicitamente.
     */
    val programma: List<GiornoProgramma> = emptyList(),

    val immagini: List<ImmagineResponse> = emptyList()
)