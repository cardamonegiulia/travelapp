package com.unical.travelapp.backend.catalog.dto;

import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import lombok.Data;

import java.math.BigDecimal;
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
    private Integer maxPartecipanti;
    private String stato;

    // galleria dell'itinerario: la prima immagine e' la copertina.
    // Sempre presente, eventualmente vuota.
    private List<ImmagineResponse> immagini = new ArrayList<>();
}