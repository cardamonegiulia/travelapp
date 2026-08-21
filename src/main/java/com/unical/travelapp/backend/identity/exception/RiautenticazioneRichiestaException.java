package com.unical.travelapp.backend.identity.exception;

/**
 * Il token e' valido ma l'autenticazione che lo ha prodotto e' troppo vecchia per
 * un'operazione sensibile.
 *
 * <p>Serve a chiudere questo scenario: un token rubato resta spendibile fino alla scadenza,
 * e senza un controllo di freschezza basterebbe per cambiare la password e prendersi
 * l'account in modo definitivo. Chiedere una riautenticazione recente sposta la prova
 * dell'identita' su Keycloak, l'unico che la password la conosce.
 *
 * <p>Viene mappata su 401 con l'header {@code WWW-Authenticate} previsto da RFC 9470
 * (step-up authentication): il client deve rifare il login con {@code max_age}, non
 * limitarsi a rinnovare il token col refresh — il refresh non cambia {@code auth_time}.
 */
public class RiautenticazioneRichiestaException extends RuntimeException {

    private final long etaMassimaSecondi;

    public RiautenticazioneRichiestaException(long etaMassimaSecondi) {
        super("autenticazione troppo vecchia per l'operazione richiesta");
        this.etaMassimaSecondi = etaMassimaSecondi;
    }

    public long getEtaMassimaSecondi() {
        return etaMassimaSecondi;
    }
}
