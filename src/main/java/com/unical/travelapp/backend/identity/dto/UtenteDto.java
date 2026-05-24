package com.unical.travelapp.backend.identity.dto;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UtenteDto {

    @NotBlank(message = "L'ID di Keycloak è obbligatorio")
    private String keycloakId;

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il nome deve avere tra i 2 e i 50 caratteri")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il cognome deve avere tra i 2 e i 50 caratteri")
    private String cognome;


    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Inserire un indirizzo email valido")//@Email controlla la forma (la chiocciola)
    @Size(max = 100, message = "L'email non può superare i 100 caratteri")  //@Size controlla la lunghezza
    private String email;

    private Ruolo ruolo;
}