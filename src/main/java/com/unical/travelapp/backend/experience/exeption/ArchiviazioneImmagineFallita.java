package com.unical.travelapp.backend.experience.exeption;


public class ArchiviazioneImmagineFallita extends RuntimeException {
    public ArchiviazioneImmagineFallita(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchiviazioneImmagineFallita(String message) {
        super(message);
    }
}
