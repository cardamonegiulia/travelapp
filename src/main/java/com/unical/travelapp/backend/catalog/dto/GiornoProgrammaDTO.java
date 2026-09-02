package com.unical.travelapp.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Una giornata del programma dell'itinerario, sia in lettura sia in scrittura.
 *
 * <p>In scrittura il campo {@code giorno} viene ignorato: la numerazione la assegna il
 * server dalla posizione nell'elenco, cosi' un client non puo' inviare due "Giorno 2" o
 * saltare il primo.
 */
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
