package com.unical.travelapp.backend.identity.exception;


public class UtenteNonTrovatoException extends RuntimeException {

    public UtenteNonTrovatoException(String messaggio) {
        super(messaggio);
    }
}