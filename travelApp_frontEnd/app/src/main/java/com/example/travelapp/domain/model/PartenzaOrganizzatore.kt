package com.example.travelapp.domain.model

/**
 * Una partenza di un itinerario vista da chi l'ha organizzata.
 *
 * Il backend restituisce solo le partenze non ancora concluse, gia' ordinate dalla piu'
 * vicina: la schermata non filtra e non riordina nulla.
 */
data class PartenzaOrganizzatore(
    val disponibilitaId: Long,
    val dataInizio: String?,
    val dataFine: String?,
    val postiDisponibili: Int?,
    val numeroPrenotazioni: Long,
    val partecipantiTotali: Long
) {
    /** Nessuno ha ancora comprato questa partenza. */
    val senzaPrenotazioni: Boolean
        get() = numeroPrenotazioni == 0L
}

/**
 * Un viaggiatore prenotato su una partenza, nella vista dell'organizzatore.
 *
 * Niente email o altri contatti: le risposte sulle prenotazioni non portano anagrafica
 * oltre al nome, ed e' una regola verificata lato backend.
 */
data class PrenotatoPartenza(
    val prenotazioneId: Long,
    val nome: String,
    val cognome: String,
    val numeroPartecipanti: Int,
    val prezzoTotale: Double,
    val statoPrenotazione: StatoPrenotazione,
    val statoPagamento: StatoPagamento?,
    val dataPrenotazione: String
) {
    val nomeCompleto: String
        get() = "$nome $cognome".trim()
}
