package com.unical.travelapp.backend.catalog.dto;

import lombok.Data;

import java.time.LocalDateTime;

// Vista di una sessione prenotabile: come per le disponibilita' degli itinerari, l'entita'
// non va esposta perche' rimanda all'attivita', che contiene a sua volta le sessioni.
@Data
public class SessioneSingolaAttivitaDTO {

    private Long id;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private Integer postiDisponibili;
    private String stato;
}
