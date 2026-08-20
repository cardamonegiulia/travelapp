package com.unical.travelapp.backend.experience.exeption;

// L'upload e' stato rifiutato dai controlli del service (dimensione, estensione, tipo reale
// del contenuto): e' un errore del chiamante, si traduce in 400.
public class ImmagineNonValida extends RuntimeException {
    public ImmagineNonValida(String message) {
        super(message);
    }
}
