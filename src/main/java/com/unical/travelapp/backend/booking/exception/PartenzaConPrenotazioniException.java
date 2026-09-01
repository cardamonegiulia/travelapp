package com.unical.travelapp.backend.booking.exception;

/**
 * Si tenta di eliminare una partenza su cui esistono prenotazioni.
 *
 * <p>Non e' un errore del client ma un conflitto con lo stato del sistema: la partenza c'e'
 * ed e' sua, solo che qualcuno l'ha comprata.
 */
public class PartenzaConPrenotazioniException extends RuntimeException {

    public PartenzaConPrenotazioniException(String message) {
        super(message);
    }
}
