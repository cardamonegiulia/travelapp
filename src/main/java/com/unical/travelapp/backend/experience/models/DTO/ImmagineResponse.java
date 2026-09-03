package com.unical.travelapp.backend.experience.models.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;


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
