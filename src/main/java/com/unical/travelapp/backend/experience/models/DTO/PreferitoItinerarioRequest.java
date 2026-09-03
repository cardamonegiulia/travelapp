package com.unical.travelapp.backend.experience.models.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class PreferitoItinerarioRequest {

    @NotNull(message = "itinerarioId è obbligatorio")
    @Positive(message = "itinerarioId non valido")
    private Long itinerarioId;
}
