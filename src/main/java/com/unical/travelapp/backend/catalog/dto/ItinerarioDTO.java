package com.unical.travelapp.backend.catalog.dto;

import lombok.Data;
import java.math.BigDecimal;

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
}