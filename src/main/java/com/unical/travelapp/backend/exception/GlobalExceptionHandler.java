package com.unical.travelapp.backend.exception;

import com.unical.travelapp.backend.booking.exception.*;
import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import com.unical.travelapp.backend.catalog.exception.SingolaAttivitaNonTrovataException;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.config.CorrelationIdFilter;
import com.unical.travelapp.backend.experience.exeption.ArchiviazioneImmagineFallita;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonValida;
import com.unical.travelapp.backend.experience.exeption.ItinerarioNonTrovato;
import com.unical.travelapp.backend.experience.exeption.ListaPreferitiNonTrovata;
import com.unical.travelapp.backend.experience.exeption.NotificaNonTrovata;
import com.unical.travelapp.backend.experience.exeption.PrenotazioneNonTrovata;
import com.unical.travelapp.backend.experience.exeption.RecensioneNonTrovata;
import com.unical.travelapp.backend.identity.exception.IdentityProviderNonDisponibileException;
import com.unical.travelapp.backend.identity.exception.PasswordNonConformeException;
import com.unical.travelapp.backend.identity.exception.RegistrazioneNonDisponibileException;
import com.unical.travelapp.backend.identity.exception.RiautenticazioneRichiestaException;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditLogger auditLogger;

    public GlobalExceptionHandler(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @ExceptionHandler(UtenteGiaEsistenteException.class)
    public ResponseEntity<ProblemDetail> handleUtenteGiaEsistente(UtenteGiaEsistenteException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Risorsa già esistente", ex.getMessage(), "risorsa-esistente", request);
    }

    @ExceptionHandler(UtenteNonTrovatoException.class)
    public ResponseEntity<ProblemDetail> handleUtenteNonTrovato(UtenteNonTrovatoException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(RegistrazioneNonDisponibileException.class)
    public ResponseEntity<ProblemDetail> handleRegistrazioneNonDisponibile(RegistrazioneNonDisponibileException ex, HttpServletRequest request) {
        log.error("Registrazione non disponibile su {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "Servizio non disponibile",
                "Registrazione temporaneamente non disponibile, riprovare più tardi", "servizio-non-disponibile", request);
    }

    @ExceptionHandler(IdentityProviderNonDisponibileException.class)
    public ResponseEntity<ProblemDetail> handleIdentityProviderNonDisponibile(IdentityProviderNonDisponibileException ex, HttpServletRequest request) {
        log.error("Operazione sull'identity provider fallita su {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "Servizio non disponibile",
                "Operazione temporaneamente non disponibile, riprovare più tardi", "servizio-non-disponibile", request);
    }

    @ExceptionHandler(PasswordNonConformeException.class)
    public ResponseEntity<ProblemDetail> handlePasswordNonConforme(PasswordNonConformeException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Dati non validi", ex.getMessage(), "password-non-conforme", request);
    }

    @ExceptionHandler(RiautenticazioneRichiestaException.class)
    public ResponseEntity<ProblemDetail> handleRiautenticazioneRichiesta(RiautenticazioneRichiestaException ex, HttpServletRequest request) {
        auditLogger.failure("RIAUTENTICAZIONE_RICHIESTA", "endpoint",
                request.getMethod() + " " + request.getRequestURI(), ex.getMessage());

        ProblemDetail pd = buildProblemDetail(HttpStatus.UNAUTHORIZED, "Riautenticazione richiesta",
                "L'operazione richiede un'autenticazione recente: ripetere il login",
                "riautenticazione-richiesta", request);
        pd.setProperty("maxAge", ex.getEtaMassimaSecondi());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer error=\"insufficient_user_authentication\", "
                                + "error_description=\"A recent authentication is required\", "
                                + "max_age=\"" + ex.getEtaMassimaSecondi() + "\"")
                .body(pd);
    }

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

    @ExceptionHandler(RichiestaPrenotazioneNonValidaException.class)
    public ResponseEntity<ProblemDetail> handleRichiestaPrenotazioneNonValida(RichiestaPrenotazioneNonValidaException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida", ex.getMessage(), "richiesta-non-valida", request);
    }

    @ExceptionHandler(AttivitaExtraNonValidaException.class)
    public ResponseEntity<ProblemDetail> handleAttivitaExtraNonValida(AttivitaExtraNonValidaException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida", ex.getMessage(), "attivita-extra-non-valida", request);
    }

    @ExceptionHandler(DisponibilitaNonTrovataException.class)
    public ResponseEntity<ProblemDetail> handleDisponibilitaNonTrovata(DisponibilitaNonTrovataException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(PrenotazioneNonTrovataException.class)
    public ResponseEntity<ProblemDetail> handlePrenotazioneNonTrovata(PrenotazioneNonTrovataException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(PagamentoNonTrovatoException.class)
    public ResponseEntity<ProblemDetail> handlePagamentoNonTrovato(PagamentoNonTrovatoException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(PostiInsufficientiException.class)
    public ResponseEntity<ProblemDetail> handlePostiInsufficienti(PostiInsufficientiException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Conflitto", ex.getMessage(), "posti-insufficienti", request);
    }

    @ExceptionHandler({
            StatoPrenotazioneNonValidoException.class,
            StatoPagamentoNonValidoException.class,
            PartenzaConPrenotazioniException.class
    })
    public ResponseEntity<ProblemDetail> handleStatoNonValido(
            RuntimeException ex,
            HttpServletRequest request) {

        return respond(
                HttpStatus.CONFLICT,
                "Conflitto",
                ex.getMessage(),
                "stato-non-valido",
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessaggioNonLeggibile(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getCause() instanceof InvalidFormatException causa
                && causa.getTargetType() != null && causa.getTargetType().isEnum()) {

            ProblemDetail pd = buildProblemDetail(HttpStatus.BAD_REQUEST, "Dati non validi",
                    "Uno o più campi della richiesta non superano la validazione", "validazione-fallita", request);

            String campo = causa.getPath().stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .filter(Objects::nonNull)
                    .reduce((primo, ultimo) -> ultimo)
                    .orElse(null);
            if (campo != null) {
                String valoriAmmessi = Arrays.stream(causa.getTargetType().getEnumConstants())
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                pd.setProperty("errori", Map.of(campo, "Valore non ammesso: i valori validi sono " + valoriAmmessi));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
        }

        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida",
                "Payload JSON non valido o con campi non previsti", "payload-non-valido", request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", "La risorsa richiesta non esiste", "risorsa-non-trovata", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violazione di integrità dei dati su {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.CONFLICT, "Conflitto sui dati",
                "La richiesta viola un vincolo di integrità dei dati", "conflitto-dati", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Conflitto", ex.getMessage(), "stato-non-valido", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida", ex.getMessage(), "richiesta-non-valida", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Accesso negato su {} {}", request.getMethod(), request.getRequestURI());
        auditLogger.failure("ACCESSO_NEGATO", "endpoint", request.getMethod() + " " + request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.FORBIDDEN, "Accesso negato",
                "Non hai i permessi necessari per eseguire questa operazione", "accesso-negato", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        auditLogger.failure("AUTENTICAZIONE_FALLITA", "endpoint", request.getMethod() + " " + request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.UNAUTHORIZED, "Autenticazione richiesta",
                "È necessario un token valido per accedere a questa risorsa", "non-autenticato", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMetodoNonAmmesso(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Metodo non consentito",
                "Il metodo HTTP usato non e' ammesso su questa risorsa", "metodo-non-consentito", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleTipoNonSupportato(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato non supportato",
                "Il formato della richiesta non e' supportato: usare application/json", "formato-non-supportato", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> handleTipoNonAccettabile(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_ACCEPTABLE, "Formato non accettabile",
                "Nessuna rappresentazione disponibile per i formati richiesti", "formato-non-accettabile", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleRottaNonMappata(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata",
                "La risorsa richiesta non esiste", "risorsa-non-trovata", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleUploadTroppoGrande(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Upload oltre il limite consentito su {} {}", request.getMethod(), request.getRequestURI());
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "Contenuto troppo grande",
                "Il contenuto inviato supera la dimensione massima consentita", "contenuto-troppo-grande", request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidazioneParametri(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errori = new HashMap<>();
        ex.getParameterValidationResults().forEach(risultato -> {
            String parametro = risultato.getMethodParameter().getParameterName();
            risultato.getResolvableErrors().forEach(errore ->
                    errori.put(parametro == null ? "parametro" : parametro, errore.getDefaultMessage()));
        });

        ProblemDetail pd = buildProblemDetail(HttpStatus.BAD_REQUEST, "Dati non validi",
                "Uno o più parametri della richiesta non superano la validazione", "validazione-fallita", request);
        pd.setProperty("errori", errori);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> handleParametroNonValido(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida",
                "Uno o piu' parametri della richiesta sono assenti o non validi", "parametro-non-valido", request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handleOrdinamentoNonValido(PropertyReferenceException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Richiesta non valida",
                "Il criterio di ordinamento richiesto non e' valido", "ordinamento-non-valido", request);
    }

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

    @ExceptionHandler(NotificaNonTrovata.class)
    public ResponseEntity<ProblemDetail> handleNotificaNonTrovata(NotificaNonTrovata ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(RecensioneNonTrovata.class)
    public ResponseEntity<ProblemDetail> handleRecensioneNonTrovata(RecensioneNonTrovata ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(ListaPreferitiNonTrovata.class)
    public ResponseEntity<ProblemDetail> handleListaPreferitiNonTrovata(ListaPreferitiNonTrovata ex, HttpServletRequest request) {
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

    @ExceptionHandler(ImmagineNonValida.class)
    public ResponseEntity<ProblemDetail> handleImmagineNonValida(ImmagineNonValida ex, HttpServletRequest request) {
        auditLogger.failure("IMMAGINE_RIFIUTATA", "endpoint",
                request.getMethod() + " " + request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "File non valido", ex.getMessage(), "immagine-non-valida", request);
    }

    @ExceptionHandler(ImmagineNonTrovata.class)
    public ResponseEntity<ProblemDetail> handleImmagineNonTrovata(ImmagineNonTrovata ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage(), "risorsa-non-trovata", request);
    }

    @ExceptionHandler(ArchiviazioneImmagineFallita.class)
    public ResponseEntity<ProblemDetail> handleArchiviazioneFallita(ArchiviazioneImmagineFallita ex, HttpServletRequest request) {
        log.error("Archiviazione immagine fallita su {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno",
                "Non e' stato possibile completare l'operazione sull'immagine", "errore-interno", request);
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
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {

        return respond(
                HttpStatus.CONFLICT,
                "Conflitto di concorrenza",
                "La risorsa è stata modificata da un'altra operazione. Riprova.",
                "conflitto-concorrenza",
                request
        );
    }
}
