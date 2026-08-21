package com.unical.travelapp.backend.identity.dto;

import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Tema;
import lombok.Data;

@Data
public class UtenteResponseDto {

    private Long id;
    private String nome;
    private String cognome;
    private String email;
    private Ruolo ruolo;
    private Tema tema;

    // null quando l'utente non ha ancora caricato una foto: il client mostra il
    // segnaposto. Dentro c'e' anche l'url da cui scaricarne il contenuto.
    private ImmagineResponse fotoProfilo;
}