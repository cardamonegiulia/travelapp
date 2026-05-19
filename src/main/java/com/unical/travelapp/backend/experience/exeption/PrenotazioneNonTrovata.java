package com.unical.travelapp.backend.experience.exeption;

public class PrenotazioneNonTrovata extends RuntimeException {
    public PrenotazioneNonTrovata(String message) {
        super(message);
    }
}
