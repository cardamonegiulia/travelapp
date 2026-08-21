package com.unical.travelapp.backend.experience.exeption;

// Errore dello storage (disco pieno, permessi, path non scrivibile): non e' colpa del
// chiamante, si traduce in 500 e il dettaglio resta nei log.
public class ArchiviazioneImmagineFallita extends RuntimeException {
    public ArchiviazioneImmagineFallita(String message, Throwable cause) {
        super(message, cause);
    }
}
