package com.unical.travelapp.backend.exception;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.config.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(Mockito.mock(AuditLogger.class));

    private MockHttpServletRequest request() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/prenotazioni/42");
        req.setMethod("GET");
        return req;
    }

    @AfterEach
    void pulisciMdc() {
        MDC.clear();
    }

    @Test
    void propagaIlTraceIdDallMdcNelProblemDetail() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "abc-123");

        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(
                new AccessDeniedException("non autorizzato"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getProperties()).containsEntry("traceId", "abc-123");
    }

    @Test
    void nonEspulgeIlMessaggioGrezzoDiUnaViolazioneDiIntegrita() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"utenti_email_key\"");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getDetail()).doesNotContain("constraint", "utenti_email_key");
    }

    @Test
    void nonEspulgeIlMessaggioGrezzoDiUnErroreGenerico() {
        ResponseEntity<ProblemDetail> response = handler.handleGenerico(
                new RuntimeException("NullPointerException su UtenteRepository riga 42"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getDetail()).doesNotContain("UtenteRepository", "NullPointerException");
    }

    @Test
    void statoApplicativoNonValidoRispondeConConflict() {
        ResponseEntity<ProblemDetail> response = handler.handleIllegalState(
                new IllegalStateException("Hai gia' recensito questa prenotazione"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getDetail()).isEqualTo("Hai gia' recensito questa prenotazione");
    }

    @Test
    void argomentoNonValidoRispondeConBadRequest() {
        ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Fornire itinerarioId oppure prenotazioneId valido"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ilProblemDetailContieneTypeTitleEInstance() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(
                new AccessDeniedException("non autorizzato"), request());

        ProblemDetail body = response.getBody();
        assertThat(body.getType()).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Accesso negato");
        assertThat(body.getInstance().toString()).isEqualTo("/api/prenotazioni/42");
        assertThat(body.getStatus()).isEqualTo(403);
    }

    @Test
    void conflittoOptimisticLockRispondeConConflict() {

        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException(
                        "DisponibilitaItinerario",
                        1L
                );

        ResponseEntity<ProblemDetail> response =
                handler.handleOptimisticLock(ex, request());

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(response.getBody().getTitle())
                .isEqualTo("Conflitto di concorrenza");

        assertThat(response.getBody().getDetail())
                .isEqualTo("La risorsa è stata modificata da un'altra operazione. Riprova.");
    }
}
