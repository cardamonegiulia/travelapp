package com.unical.travelapp.backend.experience.models.DTO;

import com.unical.travelapp.backend.experience.models.VisibilitaListaPreferiti;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class ListaPreferitiRequest {

    @NotBlank(message = "il nome della lista è obbligatorio")
    @Size(max = 80, message = "il nome della lista può avere al massimo 80 caratteri")
    @Schema(description = "Nome della lista", example = "Viaggi d'estate")
    private String nome;

    @Schema(description = "PRIVATA (default) oppure CONDIVISA", example = "PRIVATA")
    private VisibilitaListaPreferiti visibilita;

    public VisibilitaListaPreferiti visibilitaRichiesta() {
        return visibilita == null ? VisibilitaListaPreferiti.PRIVATA : visibilita;
    }
}
