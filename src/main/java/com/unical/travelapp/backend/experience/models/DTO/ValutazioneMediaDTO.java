package com.unical.travelapp.backend.experience.models.DTO;


public record ValutazioneMediaDTO(Double media, long numero) {

    public static final ValutazioneMediaDTO NESSUNA = new ValutazioneMediaDTO(null, 0L);
}
