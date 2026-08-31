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

    // Riferimenti per l'azione diretta dal client (es. aprire il form della recensione).
    // Nessun dato dell'utente: il destinatario e' sempre e solo chi sta chiamando.
    private Long prenotazioneId;
    private Long itinerarioId;
    private String titoloViaggio;
}
