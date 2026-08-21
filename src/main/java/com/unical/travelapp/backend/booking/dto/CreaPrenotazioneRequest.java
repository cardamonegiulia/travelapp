package com.unical.travelapp.backend.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreaPrenotazioneRequest {
    // il viaggiatore NON e' un campo del payload: viene sempre ricavato dal token (vedi PrenotazioneService)
    //se prenota un itinerario
    @Positive(message = "disponibilitaItinerarioId non valido")
    private Long disponibilitaItinerarioId;
    //o una singola attvita
    @Positive(message = "sessioneSingolaAttivitaId non valido")
    private Long sessioneSingolaAttivitaId;
    //i posti disponibili/quanti ne servono a lui/lei/loro/oggetto non identificato
    @NotNull(message = "Il numero di partecipanti è obbligatorio")
    @Positive(message = "Il numero di partecipanti deve essere positivo")
    private Integer numeroPartecipanti;
    //attivita opzionali
    private List<@Positive(message = "id attività extra non valido") Long> attivitaExtraIds;

}
