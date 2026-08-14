package com.unical.travelapp.backend.identity.exception;

/**
 * La registrazione non e' stata completata per un problema dell'infrastruttura di identita'
 * (Keycloak non raggiungibile, service account non autenticato, risposta inattesa dell'Admin API).
 *
 * <p>Non e' un errore del chiamante: viene mappata su 503, non su 4xx. Il messaggio serve ai
 * log lato server e non viene mai riportato nel body della risposta.
 *
 * <p>Specializza {@link IdentityProviderNonDisponibileException} per poter rispondere con un
 * messaggio riferito alla registrazione: le altre operazioni sull'IdP finirebbero altrimenti
 * per dire "registrazione non disponibile" a chi sta cancellando o aggiornando un profilo.
 */
public class RegistrazioneNonDisponibileException extends IdentityProviderNonDisponibileException {

    public RegistrazioneNonDisponibileException(String message) {
        super(message);
    }

    public RegistrazioneNonDisponibileException(String message, Throwable cause) {
        super(message, cause);
    }
}
