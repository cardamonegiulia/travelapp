package com.unical.travelapp.backend.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrePrenotazioneRequest {
    //chi prenota
    private Long viaggiatoreId;
    //se prenota un itinerario
    private Long disponibilitaItinerarioId;
    //o una singola attvita
    private Long sessioneSingolaAttivitaId;
    //i posti disponibili/quanti ne servono a lui/lei/loro/oggetto non identificato
    private Integer numeroPartecipanti;
    //attivita opzionali
    private List<Long> attivitaExtraIds;
}
