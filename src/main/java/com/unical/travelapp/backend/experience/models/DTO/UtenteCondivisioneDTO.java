package com.unical.travelapp.backend.experience.models.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "Utente che ha accesso in lettura a una lista condivisa")
public class UtenteCondivisioneDTO {

    private Long id;
    private String nome;
    private String cognome;
    private String email;
}
