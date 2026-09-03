package com.unical.travelapp.backend.identity.exception;

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
