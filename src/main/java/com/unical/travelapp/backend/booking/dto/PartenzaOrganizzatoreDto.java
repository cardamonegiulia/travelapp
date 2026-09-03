package com.unical.travelapp.backend.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartenzaOrganizzatoreDto {

    private Long disponibilitaId;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private Integer postiDisponibili;
    private long numeroPrenotazioni;
    private long partecipantiTotali;
}
