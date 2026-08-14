package com.unical.travelapp.backend.identity.exception;

/**
 * Un'operazione che doveva scrivere su Keycloak non e' andata a buon fine per un problema
 * dell'infrastruttura di identita': IdP irraggiungibile, service account non autenticato o
 * non autorizzato, risposta inattesa dell'Admin API.
 *
 * <p>Non e' un errore del chiamante: viene mappata su 503, non su 4xx. Il messaggio serve ai
 * log lato server e non viene mai riportato nel body della risposta, perche' descriverebbe
 * la configurazione del realm a chi sta sondando il servizio.
 *
 * <p>{@link RegistrazioneNonDisponibileException} ne e' la specializzazione per il flusso di
 * registrazione, che ha un messaggio utente dedicato. Le operazioni che scrivono sull'IdP per
 * altri motivi (cancellazione, aggiornamento del profilo) usano direttamente questa.
 */
public class IdentityProviderNonDisponibileException extends RuntimeException {

    public IdentityProviderNonDisponibileException(String message) {
        super(message);
    }

    public IdentityProviderNonDisponibileException(String message, Throwable cause) {
        super(message, cause);
    }
}
