package com.unical.travelapp.backend.experience.models.DTO;

import lombok.Data;

@Data
public class RecensioneDTO {

    private Long prenotazioneId;
    private Long itinerarioId;
    private Long utenteId;
    private int votazione;
    private String comm;
}
