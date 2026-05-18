package com.unical.travelapp.backend.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UtenteUpdateDto {

    @Size(min = 2, max = 50, message = "Il nome deve avere tra i 2 e i 50 caratteri")
    private String nome;

    @Size(min = 2, max = 50, message = "Il cognome deve avere tra i 2 e i 50 caratteri")
    private String cognome;

    @Email(message = "Inserire un indirizzo email valido")
    @Size(max = 100, message = "L'email non può superare i 100 caratteri")
    private String email;
}