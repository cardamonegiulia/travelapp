package com.unical.travelapp.backend.experience.exeption;


public class ListaPreferitiNonTrovata extends RuntimeException {

    public ListaPreferitiNonTrovata(String messaggio) {
        super(messaggio);
    }
}
