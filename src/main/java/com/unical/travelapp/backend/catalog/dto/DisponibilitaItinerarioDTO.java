package com.unical.travelapp.backend.catalog.dto;

import lombok.Data;

import java.time.LocalDateTime;

// Vista di una partenza per chi la deve prenotare. Non si restituisce l'entita': porta con se'
// il riferimento all'itinerario, che a sua volta contiene le disponibilita' (ciclo infinito
// in serializzazione, oltre a esporre dati di audit).
@Data
public class DisponibilitaItinerarioDTO {

    private Long id;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;

    // Termine ultimo per prenotare: assente se si prenota fino alla partenza.
    private LocalDateTime dataLimitePrenotazione;

    private Integer postiDisponibili;
}
