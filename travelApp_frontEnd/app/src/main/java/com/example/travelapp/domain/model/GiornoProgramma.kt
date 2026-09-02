package com.example.travelapp.domain.model

/**
 * Una giornata del programma dell'itinerario, cosi' come la scrive l'organizzatore e la
 * legge il viaggiatore nella scheda ("Giorno 1: Arrivo e check-in").
 *
 * [giorno] e' il progressivo assegnato dal server: in scrittura conta solo l'ordine
 * dell'elenco.
 */
data class GiornoProgramma(
    val giorno: Int,
    val titolo: String,
    val descrizione: String
)
