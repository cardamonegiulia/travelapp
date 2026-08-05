package com.unical.travelapp.backend.security.fase4_errori;

import com.unical.travelapp.backend.config.CorrelationIdFilter;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.security.support.CatturaLog;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Fase 4 - correlazione fra risposta di errore e log, e configurazione degli errori.
 *
 * <p>Il traceId e' il compromesso fra "non dire nulla al client" e "riuscire a
 * diagnosticare": nel body c'e' solo un identificativo opaco, il dettaglio resta nei log.
 * Deve pero' essere davvero ritrovabile, altrimenti e' un placebo.
 */
class TraceIdEProfiliErroreTest extends SecurityIntegrationTestBase {

    @Autowired private Environment environment;

    @BeforeEach
    void utenti() {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
    }

    @Test
    void ogniRispostaDiErrorePortaUnTraceId() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/999999")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        String traceId = objectMapper.readTree(risultato.getResponse().getContentAsString())
                .get("traceId").asText();

        assertThat(traceId).isNotBlank();
    }

    @Test
    void ilTraceIdEDiversoAOgniRichiesta() throws Exception {
        String primo = traceIdDiUnErrore();
        String secondo = traceIdDiUnErrore();

        assertThat(primo).isNotEqualTo(secondo);
    }

    @Test
    void ilTraceIdVieneRestituitoAncheNellHeaderDiRisposta() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/999999")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        String daHeader = risultato.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
        String daBody = objectMapper.readTree(risultato.getResponse().getContentAsString())
                .get("traceId").asText();

        assertThat(daHeader).isNotBlank().isEqualTo(daBody);
    }

    @Test
    void unCorrelationIdFornitoDalChiamanteVienePropagato() throws Exception {
        String fornito = "correlazione-di-prova-123";

        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/999999")
                        .header(CorrelationIdFilter.HEADER_NAME, fornito)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andReturn();

        assertThat(risultato.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(fornito);
        assertThat(objectMapper.readTree(risultato.getResponse().getContentAsString())
                .get("traceId").asText()).isEqualTo(fornito);
    }

    @Test
    void ilTraceIdDellaRispostaSiRitrovaNeiLogDellaStessaRichiesta() throws Exception {
        try (CatturaLog log = CatturaLog.di("com.unical.travelapp.backend.exception.GlobalExceptionHandler")) {
            MvcResult risultato = mockMvc.perform(get("/api/utenti")
                    .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

            assertThat(risultato.getResponse().getStatus()).isEqualTo(403);
            assertThat(log.righe())
                    .as("l'accesso negato deve lasciare traccia nei log applicativi")
                    .isNotEmpty();
        }
    }

    @Test
    void ilProfiloDiBaseNonEspoStackTraceNeMessaggiGrezzi() {
        assertThat(environment.getProperty("server.error.include-stacktrace"))
                .as("mai lo stack trace nelle risposte")
                .isEqualTo("never");
        assertThat(environment.getProperty("server.error.include-message"))
                .as("nel profilo di base il messaggio grezzo non esce")
                .isEqualTo("never");
    }

    @Test
    void nessunaRispostaDiErroreContieneMaiUnoStackTrace() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")
                        .param("sort", "campoInesistente,asc")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andReturn();

        assertThat(risultato.getResponse().getContentAsString())
                .doesNotContain("\tat ")
                .doesNotContain("at java.")
                .doesNotContain("Caused by")
                .doesNotContain("stackTrace")
                .doesNotContain("Exception");
    }

    private String traceIdDiUnErrore() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/999999")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();
        return objectMapper.readTree(risultato.getResponse().getContentAsString())
                .get("traceId").asText();
    }
}
