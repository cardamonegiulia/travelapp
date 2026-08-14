package com.unical.travelapp.backend.identity.exception;

/**
 * La registrazione non e' stata completata per un problema dell'infrastruttura di identita'
 * (Keycloak non raggiungibile, service account non autenticato, risposta inattesa dell'Admin API).
 *
 * <p>Non e' un errore del chiamante: viene mappata su 503, non su 4xx. Il messaggio serve ai
 * log lato server e non viene mai riportato nel body della risposta.
 */
public class RegistrazioneNonDisponibileException extends RuntimeException {

    public RegistrazioneNonDisponibileException(String message) {
        super(message);
    }

    public RegistrazioneNonDisponibileException(String message, Throwable cause) {
        super(message, cause);
    }
}
