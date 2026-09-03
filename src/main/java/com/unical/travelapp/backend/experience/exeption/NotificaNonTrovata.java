package com.unical.travelapp.backend.experience.exeption;


public class NotificaNonTrovata extends RuntimeException {
    public NotificaNonTrovata(String message) {
        super(message);
    }
}
