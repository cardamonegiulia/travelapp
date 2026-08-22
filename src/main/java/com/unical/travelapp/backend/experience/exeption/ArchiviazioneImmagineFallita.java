package com.unical.travelapp.backend.experience.exeption;

// Errore dello storage (disco pieno, permessi, path non scrivibile): non e' colpa del
// chiamante, si traduce in 500 e il dettaglio resta nei log.
public class ArchiviazioneImmagineFallita extends RuntimeException {
    public ArchiviazioneImmagineFallita(String message, Throwable cause) {
        super(message, cause);
    }

    // Senza causa: per i casi in cui lo storage risponde correttamente ma l'esito non e'
    // quello atteso (per esempio una chiave gia' occupata), dove non c'e' nessuna
    // eccezione sottostante da riportare.
    public ArchiviazioneImmagineFallita(String message) {
        super(message);
    }
}
