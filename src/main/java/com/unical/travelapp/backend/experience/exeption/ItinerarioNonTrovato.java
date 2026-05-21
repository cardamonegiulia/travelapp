package com.unical.travelapp.backend.experience.exeption;

public class ItinerarioNonTrovato extends RuntimeException {
    public ItinerarioNonTrovato(String message) {
        super(message);
    }
}
