package com.unical.travelapp.backend.exception;

import com.unical.travelapp.backend.catalog.exception.SingolaAttivitaNonTrovataException;
import com.unical.travelapp.backend.experience.exeption.ItinerarioNonTrovato;
import com.unical.travelapp.backend.experience.exeption.PrenotazioneNonTrovata;
import com.unical.travelapp.backend.experience.exeption.RecensioneNonTrovata;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import com.unical.travelapp.backend.booking.exception.AttivitaExtraNonValidaException;
import com.unical.travelapp.backend.booking.exception.DisponibilitaNonTrovataException;
import com.unical.travelapp.backend.booking.exception.PagamentoNonTrovatoException;
import com.unical.travelapp.backend.booking.exception.PostiInsufficientiException;
import com.unical.travelapp.backend.booking.exception.PrenotazioneNonTrovataException;
import com.unical.travelapp.backend.booking.exception.RichiestaPrenotazioneNonValidaException;
import com.unical.travelapp.backend.booking.exception.StatoPrenotazioneNonValidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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

    // 400 - Richiesta prenotazione non valida
    @ExceptionHandler(RichiestaPrenotazioneNonValidaException.class)
    public ResponseEntity<Map<String, Object>> handleRichiestaPrenotazioneNonValida(
            RichiestaPrenotazioneNonValidaException ex) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 - Attività extra non valida
    @ExceptionHandler(AttivitaExtraNonValidaException.class)
    public ResponseEntity<Map<String, Object>> handleAttivitaExtraNonValida(
            AttivitaExtraNonValidaException ex) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 404 - Disponibilità o sessione non trovata
    @ExceptionHandler(DisponibilitaNonTrovataException.class)
    public ResponseEntity<Map<String, Object>> handleDisponibilitaNonTrovata(
            DisponibilitaNonTrovataException ex) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 404 - Prenotazione non trovata
    @ExceptionHandler(PrenotazioneNonTrovataException.class)
    public ResponseEntity<Map<String, Object>> handlePrenotazioneNonTrovata(
            PrenotazioneNonTrovataException ex) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 404 - Pagamento non trovato
    @ExceptionHandler(PagamentoNonTrovatoException.class)
    public ResponseEntity<Map<String, Object>> handlePagamentoNonTrovato(
            PagamentoNonTrovatoException ex) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 409 - Posti insufficienti
    @ExceptionHandler(PostiInsufficientiException.class)
    public ResponseEntity<Map<String, Object>> handlePostiInsufficienti(
            PostiInsufficientiException ex) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 409 - Stato prenotazione/pagamento non valido
    @ExceptionHandler(StatoPrenotazioneNonValidoException.class)
    public ResponseEntity<Map<String, Object>> handleStatoPrenotazioneNonValido(
            StatoPrenotazioneNonValidoException ex) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 403 - Autenticato ma senza i permessi necessari (ruolo insufficiente o risorsa non propria)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Accesso negato");
    }

    // 401 - Autenticazione mancante o non valida
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Autenticazione richiesta");
    }

    // 400 - JSON malformato o con campi non previsti dal DTO (es. FAIL_ON_UNKNOWN_PROPERTIES)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessaggioNonLeggibile(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Payload JSON non valido o con campi non previsti");
    }

    // 500 - Fallback generico per qualsiasi altra eccezione non gestita
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex) {

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Errore interno del server");
    }

    @ExceptionHandler(PrenotazioneNonTrovata.class)
    public ResponseEntity<Map<String, Object>> handlePrenotazioneNonTrovata(PrenotazioneNonTrovata ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RecensioneNonTrovata.class)
    public ResponseEntity<Map<String, Object>> handleRecensioneNonTrovata(RecensioneNonTrovata ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ItinerarioNonTrovato.class)
    public ResponseEntity<Map<String, Object>> handleItinerarioNonTrovato(ItinerarioNonTrovato ex){
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException.class)
    public ResponseEntity<Map<String, Object>> handleItinerarioNonTrovatoException(
            com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException ex){
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SingolaAttivitaNonTrovataException.class)
    public ResponseEntity<Map<String, Object>> handleSingolaAttivitaNonTrovata(SingolaAttivitaNonTrovataException ex){
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
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