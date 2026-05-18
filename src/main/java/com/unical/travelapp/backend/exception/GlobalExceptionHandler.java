package com.unical.travelapp.backend.exception;

import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 409 - Utente già esistente (email o keycloakId duplicati)
    @ExceptionHandler(UtenteGiaEsistenteException.class)
    public ResponseEntity<Map<String, Object>> handleUtenteGiaEsistente(
            UtenteGiaEsistenteException ex) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 404 - Utente non trovato
    @ExceptionHandler(UtenteNonTrovatoException.class)
    public ResponseEntity<Map<String, Object>> handleUtenteNonTrovato(
            UtenteNonTrovatoException ex) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 400 - Validazioni fallite (@NotBlank, @Email, @Size sul DTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidazione(
            MethodArgumentNotValidException ex) {

        // Raccoglie tutti i messaggi di errore campo per campo
        Map<String, String> erroriCampi = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(errore ->
                erroriCampi.put(errore.getField(), errore.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errore", "Dati non validi");
        body.put("dettagli", erroriCampi);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 500 - Fallback generico per qualsiasi altra eccezione non gestita
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Errore interno del server");
    }

    // Metodo di supporto per costruire la risposta JSON standard
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String messaggio) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("errore", messaggio);

        return ResponseEntity.status(status).body(body);
    }
}