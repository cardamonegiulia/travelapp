package com.unical.travelapp.backend.catalog.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SingolaAttivitaDTO {

    private Long id;
    private Long organizzatoreId;
    private String titolo;
    private String descrizione;
    private String luogo;
    private BigDecimal prezzo;
    private Integer durataMinuti;
    private Integer maxPartecipanti;
}