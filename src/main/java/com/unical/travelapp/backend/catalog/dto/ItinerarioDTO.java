package com.unical.travelapp.backend.catalog.dto;

import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItinerarioDTO {

    private Long id;
    private Long organizzatoreId;
    private String titolo;
    private String descrizione;
    private String destinazionePrincipale;
    private BigDecimal prezzoBase;
    private Integer durataGiorni;

    // Periodo del viaggio: ricavato dalla disponibilità dell'itinerario, quindi assente
    // finché l'organizzatore non ne ha indicata una.
    private LocalDate dataInizio;
    private LocalDate dataFine;

    // Termine ultimo per prenotare la prima partenza: assente se non ne e' stato indicato uno.
    private LocalDate dataLimitePrenotazione;

    private Integer maxPartecipanti;
    private String stato;

    // galleria dell'itinerario: la prima immagine e' la copertina.
    // Sempre presente, eventualmente vuota.
    private List<ImmagineResponse> immagini = new ArrayList<>();
}