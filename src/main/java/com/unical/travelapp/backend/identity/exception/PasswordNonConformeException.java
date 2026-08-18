package com.unical.travelapp.backend.identity.exception;

/**
 * Keycloak ha rifiutato la password perche' non rispetta la {@code passwordPolicy} del realm.
 *
 * <p>E' un errore del chiamante (400), non un guasto: senza questa distinzione una password
 * debole diventerebbe un 503 "servizio non disponibile", e l'utente non avrebbe modo di
 * capire che deve solo sceglierne un'altra.
 *
 * <p>Il messaggio non riporta il dettaglio restituito da Keycloak: descriverebbe la policy
 * del realm a chiunque la interroghi. I requisiti sono gia' pubblicati nei messaggi di
 * validazione di {@link com.unical.travelapp.backend.identity.dto.PasswordSicura}.
 */
public class PasswordNonConformeException extends RuntimeException {

    public PasswordNonConformeException(String message) {
        super(message);
    }
}
