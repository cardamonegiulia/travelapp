package com.unical.travelapp.backend.experience.models.DTO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RecensioneResponse {

    private Long id;
    private Long prenotazioneId;
    private Long itinerarioId;
    private Long utenteId;
    private int votazione;
    private String comm;

    // foto allegate: sempre presente, eventualmente vuota
    private List<ImmagineResponse> immagini = new ArrayList<>();
}
