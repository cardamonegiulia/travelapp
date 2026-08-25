package com.unical.travelapp.backend.catalog.dto;

import java.time.LocalDateTime;

public class DisponibilitaItinerarioDTO {

    private Long id;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private Integer postiDisponibili;
    private String stato;

    public DisponibilitaItinerarioDTO() {
    }

    public DisponibilitaItinerarioDTO(
            Long id,
            LocalDateTime dataInizio,
            LocalDateTime dataFine,
            Integer postiDisponibili,
            String stato
    ) {
        this.id = id;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.postiDisponibili = postiDisponibili;
        this.stato = stato;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDateTime dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDateTime getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDateTime dataFine) {
        this.dataFine = dataFine;
    }

    public Integer getPostiDisponibili() {
        return postiDisponibili;
    }

    public void setPostiDisponibili(Integer postiDisponibili) {
        this.postiDisponibili = postiDisponibili;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }
}