package com.unical.travelapp.backend.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class GiornoProgrammaDTO {
    private Integer giorno;
    @NotBlank(message = "Il titolo della giornata è obbligatorio")
    @Size(max = 150, message = "Il titolo della giornata non può superare i 150 caratteri")
    private String titolo;
    @NotBlank(message = "La descrizione della giornata è obbligatoria")
    @Size(max = 2000, message = "La descrizione della giornata non può superare i 2000 caratteri")
    private String descrizione;
}
