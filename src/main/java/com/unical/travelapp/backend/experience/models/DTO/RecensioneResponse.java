package com.unical.travelapp.backend.experience.models.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class RecensioneResponse {

    private Long id;
    private Long prenotazioneId;
    private Long itinerarioId;

    private String itinerarioTitolo;

    private Long utenteId;
    private int votazione;
    private String comm;

    private String autoreNome;
    private String autoreCognome;

    private LocalDateTime dataRecensione;

    private List<ImmagineResponse> immagini = new ArrayList<>();
}
