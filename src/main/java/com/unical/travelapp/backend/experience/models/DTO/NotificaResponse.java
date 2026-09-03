package com.unical.travelapp.backend.experience.models.DTO;

import com.unical.travelapp.backend.experience.models.TipoNotifica;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificaResponse {

    private Long id;
    private TipoNotifica tipo;
    private String titolo;
    private String messaggio;
    private boolean letta;
    private LocalDateTime dataCreazione;

    private Long prenotazioneId;
    private Long itinerarioId;
    private String titoloViaggio;
}
