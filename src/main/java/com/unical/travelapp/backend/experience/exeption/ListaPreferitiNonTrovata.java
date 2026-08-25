package com.unical.travelapp.backend.experience.exeption;

/**
 * La lista di preferiti non esiste, oppure esiste ma chi la chiede non ha titolo per
 * vederla.
 *
 * <p>I due casi sono deliberatamente indistinguibili: rispondere 403 su una lista altrui
 * confermerebbe che quell'id esiste e a chi appartiene. Il 403 resta per il caso diverso
 * "la vedi ma non puoi modificarla", dove l'esistenza e' gia' nota a chi chiede.
 */
public class ListaPreferitiNonTrovata extends RuntimeException {

    public ListaPreferitiNonTrovata(String messaggio) {
        super(messaggio);
    }
}
