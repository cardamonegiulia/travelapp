package com.unical.travelapp.backend.common.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditLoggerTest {

    private final AuditLogger auditLogger = new AuditLogger();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        Logger auditLog = (Logger) LoggerFactory.getLogger("AUDIT");
        appender = new ListAppender<>();
        appender.start();
        auditLog.addAppender(appender);
    }

    @AfterEach
    void detachAppenderEPuliziaContesto() {
        Logger auditLog = (Logger) LoggerFactory.getLogger("AUDIT");
        auditLog.detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void unEventoDiSuccessoContieneITuttiICampiRichiesti() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("keycloak-sub-123")
                .claim("preferred_username", "mario.rossi")
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        auditLogger.success("PRENOTAZIONE_CREATA", "Prenotazione", "42");

        assertThat(appender.list).hasSize(1);
        JsonNode evento = objectMapper.readTree(appender.list.get(0).getFormattedMessage());

        assertThat(evento.get("subject").asText()).isEqualTo("keycloak-sub-123");
        assertThat(evento.get("username").asText()).isEqualTo("mario.rossi");
        assertThat(evento.get("azione").asText()).isEqualTo("PRENOTAZIONE_CREATA");
        assertThat(evento.get("risorsaTipo").asText()).isEqualTo("Prenotazione");
        assertThat(evento.get("risorsaId").asText()).isEqualTo("42");
        assertThat(evento.get("esito").asText()).isEqualTo("SUCCESS");
        assertThat(evento.has("timestamp")).isTrue();
    }

    @Test
    void unEventoDiFallimentoRiportaLEsitoEIlMotivo() throws Exception {
        auditLogger.failure("ACCESSO_NEGATO", "endpoint", "GET /api/utenti/5", "non e' il proprietario della risorsa");

        JsonNode evento = objectMapper.readTree(appender.list.get(0).getFormattedMessage());
        assertThat(evento.get("esito").asText()).isEqualTo("FAILURE");
        assertThat(evento.get("motivo").asText()).isEqualTo("non e' il proprietario della risorsa");
    }

    @Test
    void nonLoggaMaiIlTokenCompletoOAltriCampiOltreQuelliAttesi() throws Exception {
        Jwt jwt = Jwt.withTokenValue("un-token-jwt-molto-lungo-e-segreto")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("keycloak-sub-123")
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        auditLogger.success("UTENTE_CREATO", "Utente", "1");

        String riga = appender.list.get(0).getFormattedMessage();
        assertThat(riga).doesNotContain("un-token-jwt-molto-lungo-e-segreto");

        JsonNode evento = objectMapper.readTree(riga);
        List<String> chiaviAttese = List.of("timestamp", "traceId", "subject", "username",
                "azione", "risorsaTipo", "risorsaId", "esito", "ip");
        evento.fieldNames().forEachRemaining(chiave -> assertThat(chiaviAttese).contains(chiave));
    }
}
