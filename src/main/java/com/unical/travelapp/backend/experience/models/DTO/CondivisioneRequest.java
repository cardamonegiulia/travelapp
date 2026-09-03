package com.unical.travelapp.backend.experience.models.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class CondivisioneRequest {

    @Positive(message = "utenteId non valido")
    @Schema(description = "ID dell'utente con cui condividere", example = "42")
    private Long utenteId;

    @Email(message = "email non valida")
    @Schema(description = "Email dell'utente con cui condividere", example = "amico@example.com")
    private String email;
}
