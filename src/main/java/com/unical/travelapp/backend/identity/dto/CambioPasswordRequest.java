package com.unical.travelapp.backend.identity.dto;

import lombok.Data;
import lombok.ToString;

/**
 * Payload del cambio password dell'utente autenticato.
 *
 * <p>Non contiene la password attuale, e non e' una dimenticanza: verificarla lato server
 * richiederebbe di riattivare il password grant su un client Keycloak, cioe' proprio il
 * flusso che l'applicazione ha smesso di usare. Al suo posto l'endpoint pretende
 * un'autenticazione recente (claim {@code auth_time} del token), che e' la prova equivalente
 * — chi ha appena superato il login e' la stessa persona — e passa da Keycloak, l'unico che
 * la password la conosce.
 */
@Data
public class CambioPasswordRequest {

    /** Esclusa da {@code toString()}: vedi la nota in {@link RegistrazioneRequest}. */
    @ToString.Exclude
    @PasswordSicura
    private String nuovaPassword;
}
