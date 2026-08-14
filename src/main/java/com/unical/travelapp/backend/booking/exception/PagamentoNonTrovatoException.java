package com.unical.travelapp.backend.booking.exception;

public class PagamentoNonTrovatoException extends RuntimeException {

    public PagamentoNonTrovatoException(String message) {
        super(message);
    }
}