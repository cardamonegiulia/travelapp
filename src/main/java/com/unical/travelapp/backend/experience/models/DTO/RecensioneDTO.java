package com.unical.travelapp.backend.experience.models.DTO;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.identity.entity.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class RecensioneDTO {

    public Prenotazione preno;
    private Utente ut;
    private int votazione;
    private String comm;
}
