package com.unical.travelapp.backend.experience.models.DTO;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.identity.entity.Utente;

import lombok.Data;

@Data
public class RecensioneDTO {

    private Prenotazione preno;
    private Utente ut;
    private int votazione;
    private String comm;

    public Long getPrenotazioneId() {
        return preno.getId();
    }
}
