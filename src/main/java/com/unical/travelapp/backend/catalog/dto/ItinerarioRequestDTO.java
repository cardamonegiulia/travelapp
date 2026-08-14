package com.unical.travelapp.backend.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

// DTO di request: niente id/organizzatoreId/stato, sono gestiti dal server (mai dal client)
@Data
public class ItinerarioRequestDTO {

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 150, message = "Il titolo non può superare i 150 caratteri")
    private String titolo;

    @Size(max = 5000, message = "La descrizione non può superare i 5000 caratteri")
    private String descrizione;

    @NotBlank(message = "La destinazione principale è obbligatoria")
    @Size(max = 150, message = "La destinazione non può superare i 150 caratteri")
    private String destinazionePrincipale;

    @NotNull(message = "Il prezzo base è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "Il prezzo base deve essere positivo")
    private BigDecimal prezzoBase;

    @NotNull(message = "La durata in giorni è obbligatoria")
    @Positive(message = "La durata deve essere positiva")
    private Integer durataGiorni;

    @NotNull(message = "Il numero massimo di partecipanti è obbligatorio")
    @Positive(message = "Il numero massimo di partecipanti deve essere positivo")
    private Integer maxPartecipanti;
}
