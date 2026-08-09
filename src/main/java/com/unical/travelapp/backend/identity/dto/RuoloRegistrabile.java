package com.unical.travelapp.backend.identity.dto;

import com.unical.travelapp.backend.identity.entity.Ruolo;

/**
 * Ruoli che un utente puo' scegliere da solo in fase di registrazione.
 *
 * <p>E' volutamente un enum <b>separato</b> da {@link Ruolo} e non un sottoinsieme di
 * {@link Ruolo} validato a runtime: ADMIN non e' proprio rappresentabile in questo tipo,
 * quindi un payload con {@code "ruolo":"ADMIN"} viene rifiutato in fase di deserializzazione
 * (400) prima ancora di raggiungere il service. Un controllo applicativo, al contrario, si
 * puo' dimenticare aggiungendolo su un nuovo endpoint; questo vincolo no, lo impone il tipo.
 */
public enum RuoloRegistrabile {

    VIAGGIATORE(Ruolo.VIAGGIATORE),
    ORGANIZZATORE(Ruolo.ORGANIZZATORE);

    private final Ruolo ruolo;

    RuoloRegistrabile(Ruolo ruolo) {
        this.ruolo = ruolo;
    }

    /** Ruolo corrispondente nel dominio applicativo (colonna {@code utenti.ruolo}). */
    public Ruolo toRuolo() {
        return ruolo;
    }

    /** Nome del ruolo realm da assegnare su Keycloak: stessa stringa del ruolo locale. */
    public String nomeRuoloRealm() {
        return ruolo.name();
    }
}
