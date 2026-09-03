package com.unical.travelapp.backend.identity.exception;

public class RegistrazioneNonDisponibileException extends IdentityProviderNonDisponibileException {

    public RegistrazioneNonDisponibileException(String message) {
        super(message);
    }

    public RegistrazioneNonDisponibileException(String message, Throwable cause) {
        super(message, cause);
    }
}
