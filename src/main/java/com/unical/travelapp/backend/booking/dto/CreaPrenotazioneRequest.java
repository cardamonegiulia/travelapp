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
    @Positive(message = "disponibilitaItinerarioId non valido")
    private Long disponibilitaItinerarioId;
    @Positive(message = "sessioneSingolaAttivitaId non valido")
    private Long sessioneSingolaAttivitaId;
    @NotNull(message = "Il numero di partecipanti è obbligatorio")
    @Positive(message = "Il numero di partecipanti deve essere positivo")
    private Integer numeroPartecipanti;
    private List<@Positive(message = "id attività extra non valido") Long> attivitaExtraIds;

}
