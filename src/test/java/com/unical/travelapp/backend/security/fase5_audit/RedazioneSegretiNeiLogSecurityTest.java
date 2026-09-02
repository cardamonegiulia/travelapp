package com.unical.travelapp.backend.security.fase5_audit;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.security.support.CatturaLog;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.ServerJwkDiProva;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Fase 5 - redazione dei segreti nei log. E' il test piu' importante della fase: un token
 * finito nei log e' una credenziale valida a disposizione di chiunque legga i log, e i log
 * di solito hanno una platea molto piu' ampia del database.
 *
 * <p>Si verificano sia gli appender in memoria sia il file logs/audit.log scritto davvero.
 */
class RedazioneSegretiNeiLogSecurityTest extends SecurityIntegrationTestBase {

    private static final String PASSWORD_IN_CHIARO = "SuperSegreta123!";
    private static final Path FILE_AUDIT = Path.of("logs", "audit.log");

    @DynamicPropertySource
    static void chiaviPubblicheLocali(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> ServerJwkDiProva.istanza().jwkSetUri());
    }

    private long dimensioneAuditPrimaDelTest;

    @BeforeEach
    void utentiEPosizioneNelFile() throws IOException {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        dimensioneAuditPrimaDelTest = Files.exists(FILE_AUDIT) ? Files.size(FILE_AUDIT) : 0L;
    }

    /** Solo le righe di audit scritte durante questo test. */
    private String audltScrittoDuranteIlTest() throws IOException {
        if (!Files.exists(FILE_AUDIT)) {
            return "";
        }
        byte[] tutto = Files.readAllBytes(FILE_AUDIT);
        int da = (int) Math.min(dimensioneAuditPrimaDelTest, tutto.length);
        return new String(tutto, da, tutto.length - da, StandardCharsets.UTF_8);
    }

    private String tokenRealistico() {
        return ServerJwkDiProva.istanza().idp().token(
                TestJwt.ISSUER, List.of(TestJwt.CLIENT_ID), SUB_UTENTE_A,
                Map.of("realm_access", Map.of("roles", List.of("VIAGGIATORE")),
                        "preferred_username", "utente.a"));
    }

    @Test
    void ilTokenBearerNonCompareMaiNeiLogNeNelFileDiAudit() throws Exception {
        String token = tokenRealistico();

        try (CatturaLog root = CatturaLog.root(); CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(post("/api/utenti/me").header("Authorization", "Bearer " + token)).andReturn();
            mockMvc.perform(get("/api/utenti").header("Authorization", "Bearer " + token)).andReturn();

            assertThat(root.testoCompleto())
                    .as("il token non deve comparire nel log applicativo")
                    .doesNotContain(token)
                    .doesNotContain("Bearer ");
            assertThat(audit.testoCompleto())
                    .as("il token non deve comparire negli eventi di audit")
                    .doesNotContain(token)
                    .doesNotContain("Bearer ");
        }

        assertThat(audltScrittoDuranteIlTest())
                .as("il token non deve finire nel file logs/audit.log")
                .doesNotContain(token)
                .doesNotContain("Bearer ");
    }

    @Test
    void unaPortionDelTokenNonCompareNeiLog() throws Exception {
        String token = tokenRealistico();
        String firma = token.substring(token.lastIndexOf('.') + 1);
        String payload = token.split("\\.")[1];

        try (CatturaLog root = CatturaLog.root(); CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(get("/api/utenti").header("Authorization", "Bearer " + token)).andReturn();

            for (CatturaLog log : List.of(root, audit)) {
                assertThat(log.testoCompleto())
                        .as("nemmeno un frammento del token deve finire nei log")
                        .doesNotContain(firma)
                        .doesNotContain(payload);
            }
        }
    }

    @Test
    void unaPasswordNelPayloadNonCompareNeiLog() throws Exception {
        try (CatturaLog root = CatturaLog.root(); CatturaLog audit = CatturaLog.audit()) {
            // payload con un campo password: il DTO non lo prevede, quindi la richiesta
            // fallisce - ma il valore non deve comunque finire nei log
            mockMvc.perform(put("/api/utenti/"
                            + utenteRepository.findByKeycloakId(SUB_UTENTE_A).orElseThrow().getId())
                            .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Ada\",\"password\":\"" + PASSWORD_IN_CHIARO + "\"}"))
                    .andReturn();

            assertThat(root.testoCompleto())
                    .as("il valore della password non deve comparire nel log applicativo")
                    .doesNotContain(PASSWORD_IN_CHIARO);
            assertThat(audit.testoCompleto())
                    .doesNotContain(PASSWORD_IN_CHIARO);
        }

        assertThat(audltScrittoDuranteIlTest()).doesNotContain(PASSWORD_IN_CHIARO);
    }

    @Test
    void unAutenticazioneFallitaNonLoggaIlTokenPresentato() throws Exception {
        // token con audience sbagliata: viene respinto, ma non deve essere trascritto
        String tokenRespinto = ServerJwkDiProva.istanza().idp().token(
                TestJwt.ISSUER, List.of("account"), SUB_UTENTE_A, Map.of());

        try (CatturaLog root = CatturaLog.root(); CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(get("/api/utenti").header("Authorization", "Bearer " + tokenRespinto))
                    .andReturn();

            assertThat(root.testoCompleto())
                    .as("un tentativo fallito non deve lasciare il token nei log")
                    .doesNotContain(tokenRespinto);
            assertThat(audit.testoCompleto()).doesNotContain(tokenRespinto);
        }

        assertThat(audltScrittoDuranteIlTest()).doesNotContain(tokenRespinto);
    }

    @Test
    void lHeaderAuthorizationNonVieneMaiTrascritto() throws Exception {
        try (CatturaLog root = CatturaLog.root(); CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(get("/api/utenti")
                            .header("Authorization", "Bearer " + tokenRealistico())
                            .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                    .andReturn();

            for (CatturaLog log : List.of(root, audit)) {
                assertThat(log.testoCompleto().toLowerCase())
                        .as("il nome dell'header non deve comparire con il suo valore")
                        .doesNotContain("authorization:")
                        .doesNotContain("authorization=");
            }
        }
    }

    @Test
    void gliEventiDiAuditContengonoIlSubjectMaMaiIlTokenCheLoTrasportava() throws Exception {
        String token = tokenRealistico();

        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(post("/api/itinerari")
                            .header("Authorization", "Bearer " + ServerJwkDiProva.istanza().idp().token(
                                    TestJwt.ISSUER, List.of(TestJwt.CLIENT_ID), SUB_ORGANIZZATORE,
                                    Map.of("realm_access", Map.of("roles", List.of("ORGANIZZATORE")))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"Con token\",\"destinazionePrincipale\":\"D\","
                                    + "\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}"))
                    .andReturn();

            String testo = audit.testoCompleto();
            assertThat(testo)
                    .as("il subject serve e va registrato")
                    .contains(SUB_ORGANIZZATORE);
            assertThat(testo)
                    .as("il token che lo trasportava no")
                    .doesNotContain(token)
                    .doesNotContain("eyJ");
        }
    }

    @Test
    void ilFileDiAuditNonContieneMaiUnTokenJwtInQualsiasiForma() throws Exception {
        mockMvc.perform(post("/api/utenti/me").header("Authorization", "Bearer " + tokenRealistico()));
        mockMvc.perform(get("/api/utenti").header("Authorization", "Bearer " + tokenRealistico()));

        String contenuto = audltScrittoDuranteIlTest();

        // "eyJ" e' il prefisso Base64 di ogni header JWT: se compare, un token e' finito nel file
        assertThat(contenuto)
                .as("nessun frammento riconoscibile di JWT nel file di audit")
                .doesNotContain("eyJ");
    }
}
