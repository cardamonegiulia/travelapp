package com.unical.travelapp.backend.experience.models.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Utente con cui una lista di preferiti e' condivisa, nella forma minima che serve a
 * mostrarlo: nessun keycloakId, nessun ruolo, nessun dato che il destinatario della
 * risposta non debba vedere.
 */
@Data
@Schema(description = "Utente che ha accesso in lettura a una lista condivisa")
public class UtenteCondivisioneDTO {

    private Long id;
    private String nome;
    private String cognome;
    private String email;
}
