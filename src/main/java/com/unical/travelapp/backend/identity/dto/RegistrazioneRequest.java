package com.unical.travelapp.backend.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/**
 * Payload della registrazione self-service.
 *
 * <p>DTO dedicato, mai l'entita' JPA: l'utente non deve poter valorizzare {@code id},
 * {@code keycloakId} o campi di audit (con
 * {@code spring.jackson.deserialization.fail-on-unknown-properties=true} un campo estraneo
 * fa fallire la richiesta con 400 invece di essere ignorato in silenzio).
 */
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

    /**
     * Esclusa da {@code toString()}: senza questo, il {@code toString()} generato da
     * {@code @Data} stamperebbe la password in chiaro in qualsiasi log che registri il DTO
     * (messaggio di errore, breakpoint, log di debug di un framework).
     */
    @ToString.Exclude
    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 12, max = 128, message = "La password deve avere tra i 12 e i 128 caratteri")
    @Pattern(
            regexp = ".*[A-Za-z].*",
            message = "La password deve contenere almeno una lettera"
    )
    @Pattern(
            regexp = ".*\\d.*",
            message = "La password deve contenere almeno un numero"
    )
    private String password;

    /**
     * Solo VIAGGIATORE o ORGANIZZATORE: il tipo {@link RuoloRegistrabile} non contempla
     * ADMIN, quindi richiederlo produce 400 e non c'e' modo di ottenerlo dalla registrazione.
     */
    @NotNull(message = "Il ruolo è obbligatorio: VIAGGIATORE oppure ORGANIZZATORE")
    private RuoloRegistrabile ruolo;
}
