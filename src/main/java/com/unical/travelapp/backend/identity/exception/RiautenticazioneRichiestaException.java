package com.unical.travelapp.backend.identity.exception;

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
