package com.unical.travelapp.backend.experience.models;

/**
 * Visibilita' di una lista di itinerari preferiti, come da specifica del viaggiatore:
 * ogni lista e' privata oppure condivisa con specifici utenti.
 *
 * <p>Non esiste un valore "pubblica": una lista non e' mai visibile a tutti gli utenti
 * della piattaforma. "Condivisa" significa accessibile ai soli destinatari scelti dal
 * proprietario, uno per uno.
 */
public enum VisibilitaListaPreferiti {

    /** Visibile soltanto al viaggiatore che l'ha creata. */
    PRIVATA,

    /** Visibile al proprietario e ai soli utenti presenti fra i destinatari. */
    CONDIVISA
}
