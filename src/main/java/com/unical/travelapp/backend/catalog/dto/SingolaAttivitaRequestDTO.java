package com.unical.travelapp.backend.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

// DTO di request: niente id/organizzatoreId, sono gestiti dal server (mai dal client)
@Data
public class SingolaAttivitaRequestDTO {

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 150, message = "Il titolo non può superare i 150 caratteri")
    private String titolo;

    @Size(max = 5000, message = "La descrizione non può superare i 5000 caratteri")
    private String descrizione;

    @NotBlank(message = "Il luogo è obbligatorio")
    @Size(max = 150, message = "Il luogo non può superare i 150 caratteri")
    private String luogo;

    @NotNull(message = "Il prezzo è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "Il prezzo deve essere positivo")
    private BigDecimal prezzo;

    @NotNull(message = "La durata in minuti è obbligatoria")
    @Positive(message = "La durata deve essere positiva")
    private Integer durataMinuti;

    @NotNull(message = "Il numero massimo di partecipanti è obbligatorio")
    @Positive(message = "Il numero massimo di partecipanti deve essere positivo")
    private Integer maxPartecipanti;
}
