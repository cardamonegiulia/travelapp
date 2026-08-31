package com.unical.travelapp.backend.experience.exeption;

/**
 * Notifica inesistente, oppure esistente ma di un altro utente: i due casi rispondono allo
 * stesso modo (404), altrimenti la differenza fra 403 e 404 direbbe a un estraneo quali id
 * esistono.
 */
public class NotificaNonTrovata extends RuntimeException {
    public NotificaNonTrovata(String message) {
        super(message);
    }
}
