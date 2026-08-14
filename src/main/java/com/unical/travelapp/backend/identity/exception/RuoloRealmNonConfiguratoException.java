package com.unical.travelapp.backend.identity.exception;

/**
 * Il ruolo realm che la registrazione deve assegnare non esiste su Keycloak.
 *
 * <p>Errore di configurazione dell'ambiente, non della richiesta. Ha un tipo dedicato perche'
 * l'alternativa - proseguire e creare comunque l'utente - produrrebbe un account senza ruolo:
 * un fallimento silenzioso che si manifesta molto piu' tardi come 403 inspiegabile sugli
 * endpoint protetti da {@code hasRole(...)}, con nessuna traccia della causa.
 */
public class RuoloRealmNonConfiguratoException extends RegistrazioneNonDisponibileException {

    private final String nomeRuolo;

    public RuoloRealmNonConfiguratoException(String nomeRuolo) {
        super("Il ruolo realm '" + nomeRuolo + "' non esiste su Keycloak: registrazione interrotta");
        this.nomeRuolo = nomeRuolo;
    }

    public String getNomeRuolo() {
        return nomeRuolo;
    }
}
