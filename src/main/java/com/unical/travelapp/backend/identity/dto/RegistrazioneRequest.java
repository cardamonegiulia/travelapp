package com.unical.travelapp.backend.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class RegistrazioneRequest {

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il nome deve avere tra i 2 e i 50 caratteri")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il cognome deve avere tra i 2 e i 50 caratteri")
    private String cognome;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Inserire un indirizzo email valido")
    @Size(max = 100, message = "L'email non può superare i 100 caratteri")
    private String email;

    @ToString.Exclude
    @PasswordSicura
    private String password;

    @NotNull(message = "Il ruolo è obbligatorio: VIAGGIATORE oppure ORGANIZZATORE")
    private RuoloRegistrabile ruolo;
}
