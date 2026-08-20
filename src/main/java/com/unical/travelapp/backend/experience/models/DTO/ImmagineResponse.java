package com.unical.travelapp.backend.experience.models.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

// Risposta restituita al client dopo l'upload: contiene il link da cui scaricare
// l'immagine, non il percorso sullo storage (che e' un dettaglio interno).
@Data
public class ImmagineResponse {

    private Long id;

    @Schema(description = "URL da cui scaricare il contenuto dell'immagine",
            example = "/api/immagini/123/contenuto")
    private String url;

    private String contentType;
    private long dimensioneByte;
    private int larghezza;
    private int altezza;
    private Long proprietarioId;
    private LocalDateTime caricataIl;
}
