package com.unical.travelapp.backend.identity.dto;

import com.unical.travelapp.backend.identity.entity.Ruolo;

public enum RuoloRegistrabile {

    VIAGGIATORE(Ruolo.VIAGGIATORE),
    ORGANIZZATORE(Ruolo.ORGANIZZATORE);

    private final Ruolo ruolo;

    RuoloRegistrabile(Ruolo ruolo) {
        this.ruolo = ruolo;
    }

    public Ruolo toRuolo() {
        return ruolo;
    }

    public String nomeRuoloRealm() {
        return ruolo.name();
    }
}
