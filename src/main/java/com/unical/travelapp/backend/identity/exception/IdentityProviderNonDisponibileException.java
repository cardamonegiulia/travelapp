package com.unical.travelapp.backend.identity.exception;

public class IdentityProviderNonDisponibileException extends RuntimeException {

    public IdentityProviderNonDisponibileException(String message) {
        super(message);
    }

    public IdentityProviderNonDisponibileException(String message, Throwable cause) {
        super(message, cause);
    }
}
