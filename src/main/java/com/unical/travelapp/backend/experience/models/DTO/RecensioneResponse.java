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
    private Long utenteId;
    private int votazione;
    private String comm;

    // Chi ha scritto la recensione, come lo si mostra sotto l'itinerario: nome e cognome,
    // mai l'email o l'identificativo dell'identity provider.
    private String autoreNome;
    private String autoreCognome;

    // Quando e' stata scritta. Il nome del campo NON e' quello della colonna di audit:
    // e' un dato editoriale della recensione, non il tracciamento interno della riga.
    private LocalDateTime dataRecensione;

    // foto allegate: sempre presente, eventualmente vuota
    private List<ImmagineResponse> immagini = new ArrayList<>();
}
