package com.unical.travelapp.backend.catalog.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class DisponibilitaItinerarioDTO {
    private Long id;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private LocalDateTime dataLimitePrenotazione;
    private Integer postiDisponibili;
}
