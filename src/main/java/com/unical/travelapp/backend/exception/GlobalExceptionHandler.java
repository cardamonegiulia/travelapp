package com.unical.travelapp.backend.exception;

import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import com.unical.travelapp.backend.catalog.exception.SingolaAttivitaNonTrovataException;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.config.CorrelationIdFilter;
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
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

// Risposte di errore in formato RFC 7807 (ProblemDetail): niente stack trace, niente
// messaggi di eccezione grezzi o dettagli infrastrutturali (nomi tabella/colonna/constraint)
// nel body. Lo stack trace completo va solo nei log server, correlabile via traceId.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditLogger auditLogger;

    public GlobalExceptionHandler(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    // 409 - Utente già esistente (email o keycloakId duplicati)
    @ExceptionHandler(UtenteGiaEsistenteException.class)
    public ResponseEntity<ProblemDetail> handleUtenteGiaEsistente(UtenteGiaEsistenteException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Risorsa già esistente", ex.getMessage(), "risorsa-esistente", request);
    }

    // 404 - Utente non trovato
    @ExceptionHandler(UtenteNonTrovatoException.class)
    public ResponseEntity<ProblemDetail> handleUtenteNonTrovato(UtenteNonTrovatoException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    // 400 - Validazioni fallite (@NotBlank, @Email, @Size sul DTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidazione(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> erroriCampi = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(errore ->
                erroriCampi.put(errore.getField(), errore.getDefaultMessage())
        );

        ProblemDetail pd = buildProblemDetail(HttpStatus.BAD_REQUEST, "Dati non validi",
                "Uno o più campi della richiesta non superano la validazione", "validazione-fallita", request);
        pd.setProperty("errori", erroriCampi);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    // 400 - Richiesta prenotazione non valida
    @ExceptionHandler(RichiestaPrenotazioneNonValidaException.class)
    public ResponseEntity<ProblemDetail> handleRichiestaPrenotazioneNonValida(RichiestaPrenotazioneNonValidaException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida", ex.getMessage(), "richiesta-non-valida", request);
    }

    // 400 - Attività extra non valida
    @ExceptionHandler(AttivitaExtraNonValidaException.class)
    public ResponseEntity<ProblemDetail> handleAttivitaExtraNonValida(AttivitaExtraNonValidaException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida", ex.getMessage(), "attivita-extra-non-valida", request);
    }

    // 404 - Disponibilità o sessione non trovata
    @ExceptionHandler(DisponibilitaNonTrovataException.class)
    public ResponseEntity<ProblemDetail> handleDisponibilitaNonTrovata(DisponibilitaNonTrovataException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    // 404 - Prenotazione non trovata
    @ExceptionHandler(PrenotazioneNonTrovataException.class)
    public ResponseEntity<ProblemDetail> handlePrenotazioneNonTrovata(PrenotazioneNonTrovataException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    // 404 - Pagamento non trovato
    @ExceptionHandler(PagamentoNonTrovatoException.class)
    public ResponseEntity<ProblemDetail> handlePagamentoNonTrovato(PagamentoNonTrovatoException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    // 409 - Posti insufficienti
    @ExceptionHandler(PostiInsufficientiException.class)
    public ResponseEntity<ProblemDetail> handlePostiInsufficienti(PostiInsufficientiException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Conflitto", ex.getMessage(), "posti-insufficienti", request);
    }

    // 409 - Stato prenotazione/pagamento non valido
    @ExceptionHandler(StatoPrenotazioneNonValidoException.class)
    public ResponseEntity<ProblemDetail> handleStatoPrenotazioneNonValido(StatoPrenotazioneNonValidoException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Conflitto", ex.getMessage(), "stato-non-valido", request);
    }

    // 400 - JSON malformato o con campi non previsti dal DTO (es. FAIL_ON_UNKNOWN_PROPERTIES)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessaggioNonLeggibile(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida",
                "Payload JSON non valido o con campi non previsti", "payload-non-valido", request);
    }

    // 404 - Riferimento a un'entità JPA non più presente (es. lazy reference risolta dopo cancellazione)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", "La risorsa richiesta non esiste", "risorsa-non-trovata", request);
    }

    // 409 - Vincolo di integrità dei dati violato (es. unique/foreign key): mai esporre dettagli DB nel body
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violazione di integrità dei dati su {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.CONFLICT, "Conflitto sui dati",
                "La richiesta viola un vincolo di integrità dei dati", "conflitto-dati", request);
    }

    // 409 - Stato applicativo incoerente con l'operazione richiesta (es. recensione già presente)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Conflitto", ex.getMessage(), "stato-non-valido", request);
    }

    // 400 - Argomenti applicativi non validi (validazioni manuali nei service)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida", ex.getMessage(), "richiesta-non-valida", request);
    }

    // 403 - Autenticato ma senza i permessi necessari (ruolo insufficiente o risorsa non propria)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Accesso negato su {} {}", request.getMethod(), request.getRequestURI());
        auditLogger.failure("ACCESSO_NEGATO", "endpoint", request.getMethod() + " " + request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.FORBIDDEN, "Accesso negato",
                "Non hai i permessi necessari per eseguire questa operazione", "accesso-negato", request);
    }

    // 401 - Autenticazione mancante o non valida
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        auditLogger.failure("AUTENTICAZIONE_FALLITA", "endpoint", request.getMethod() + " " + request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.UNAUTHORIZED, "Autenticazione richiesta",
                "È necessario un token valido per accedere a questa risorsa", "non-autenticato", request);
    }

    // 500 - Fallback generico per qualsiasi altra eccezione non gestita: nessun dettaglio grezzo,
    // lo stack trace completo finisce SOLO nei log (correlabile con traceId)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenerico(Exception ex, HttpServletRequest request) {
        log.error("Errore interno non gestito su {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno",
                "Si è verificato un errore interno del server", "errore-interno", request);
    }

    @ExceptionHandler(PrenotazioneNonTrovata.class)
    public ResponseEntity<ProblemDetail> handlePrenotazioneNonTrovata(PrenotazioneNonTrovata ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(RecensioneNonTrovata.class)
    public ResponseEntity<ProblemDetail> handleRecensioneNonTrovata(RecensioneNonTrovata ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(ItinerarioNonTrovato.class)
    public ResponseEntity<ProblemDetail> handleItinerarioNonTrovato(ItinerarioNonTrovato ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(ItinerarioNonTrovatoException.class)
    public ResponseEntity<ProblemDetail> handleItinerarioNonTrovatoException(ItinerarioNonTrovatoException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(SingolaAttivitaNonTrovataException.class)
    public ResponseEntity<ProblemDetail> handleSingolaAttivitaNonTrovata(SingolaAttivitaNonTrovataException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    private ResponseEntity<ProblemDetail> respond(HttpStatus status, String title, String detail, String typeSlug, HttpServletRequest request) {
        return ResponseEntity.status(status).body(buildProblemDetail(status, title, detail, typeSlug, request));
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail, String typeSlug, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("urn:travelapp:problem:" + typeSlug));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("traceId", MDC.get(CorrelationIdFilter.MDC_KEY));
        return problemDetail;
    }
}
