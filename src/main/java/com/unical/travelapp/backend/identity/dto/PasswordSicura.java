package com.unical.travelapp.backend.identity.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requisiti minimi di una password scelta dall'utente.
 *
 * <p>Vincolo composto e non quattro annotazioni ripetute su ogni DTO: le regole sono una
 * decisione di sicurezza, e duplicarle significa che prima o poi due punti dell'applicazione
 * accetteranno password diverse. Oggi la usano registrazione e cambio password.
 *
 * <p>Vanno tenute allineate alla {@code passwordPolicy} del realm
 * ({@code keycloak-import/travelapp-realm.json}, verificata da {@code ConfigurazioneRealmTest}):
 * una regola presente solo su Keycloak si manifesterebbe come errore alla scrittura sull'IdP
 * invece che come errore di validazione del form, che e' quello che l'utente puo' correggere.
 */
@NotBlank(message = "La password è obbligatoria")
@Size(min = 12, max = 128, message = "La password deve avere tra i 12 e i 128 caratteri")
@Pattern(regexp = ".*[A-Za-z].*", message = "La password deve contenere almeno una lettera")
@Pattern(regexp = ".*\\d.*", message = "La password deve contenere almeno un numero")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PasswordSicura {

    String message() default "La password non rispetta i requisiti minimi";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
