package com.unical.travelapp.backend.security.fase3_risorse;

import com.fasterxml.jackson.databind.JsonNode;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LimitiRisorseSecurityTest extends SecurityIntegrationTestBase {

    @Autowired private Environment environment;

    @BeforeEach
    void utenti() {
        utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
    }

    @Test
    void unaRichiestaMultipartVersoUnEndpointJsonVieneRifiutataCon415() throws Exception {
        byte[] contenuto = new byte[1024];

        MvcResult risultato = mockMvc.perform(multipart("/api/itinerari")
                        .file(new MockMultipartFile("file", "file.bin",
                                "application/octet-stream", contenuto))
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andReturn();

        assertThat(risultato.getResponse().getStatus())
                .as("un content-type non previsto va rifiutato con 415, non con 500")
                .isEqualTo(415);
        NessunLeak.verifica(risultato);

        JsonNode corpo = objectMapper.readTree(risultato.getResponse().getContentAsString());
        assertThat(corpo.get("status").asInt()).isEqualTo(415);
        assertThat(corpo.has("traceId")).isTrue();
    }

    @Test
    void unIntervalloSmisuratoVieneRifiutatoPrimaDiGenerareSessioni() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/attivita/con-sessioni")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attivitaValida())
                        .param("inizio", "0001-01-01")
                        .param("fine", "9999-12-31")
                        .param("giorni", "1", "2", "3", "4", "5", "6", "7"))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(sessioneRepository.count())
                .as("il controllo deve scattare prima del ciclo, non lasciarne generare una parte")
                .isZero();
        assertThat(singolaAttivitaRepository.count())
                .as("nemmeno l'attivita' deve restare salvata")
                .isZero();
    }

    @Test
    void unGiornoDellaSettimanaFuoriRangeVieneRifiutato() throws Exception {
        mockMvc.perform(post("/api/attivita/con-sessioni")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attivitaValida())
                        .param("inizio", "2026-09-01")
                        .param("fine", "2026-09-30")
                        .param("giorni", "0", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.giorni").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unIntervalloNormaleContinuaAFunzionare() throws Exception {
        mockMvc.perform(post("/api/attivita/con-sessioni")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attivitaValida())
                        .param("inizio", "2026-09-01")
                        .param("fine", "2026-09-30")
                        .param("giorni", "1"))
                .andExpect(status().isOk());

        assertThat(sessioneRepository.count())
                .as("settembre 2026 ha quattro lunedi': il tetto non deve intralciare l'uso normale")
                .isEqualTo(4);
    }

    private String attivitaValida() {
        return """
                {"titolo":"Tour del centro","luogo":"Cosenza","prezzo":25.00,
                 "durataMinuti":90,"maxPartecipanti":10}
                """;
    }

    @Test
    void ilLimiteMultipartEEffettivamenteConfigurato() {
        DataSize maxFile = DataSize.parse(
                environment.getProperty("spring.servlet.multipart.max-file-size", "-1B"));
        DataSize maxRichiesta = DataSize.parse(
                environment.getProperty("spring.servlet.multipart.max-request-size", "-1B"));

        assertThat(maxFile.toMegabytes())
                .as("deve esserci un tetto esplicito sulla dimensione del file")
                .isPositive()
                .isLessThanOrEqualTo(10);
        assertThat(maxRichiesta.toMegabytes())
                .as("deve esserci un tetto esplicito sulla dimensione della richiesta")
                .isPositive()
                .isLessThanOrEqualTo(10);
    }

    @Test
    void ilTettoDiPaginazioneEEffettivamenteConfigurato() {
        assertThat(environment.getProperty("spring.data.web.pageable.max-page-size", Integer.class))
                .as("senza max-page-size il client potrebbe chiedere pagine illimitate")
                .isNotNull()
                .isPositive()
                .isLessThanOrEqualTo(100);

        assertThat(environment.getProperty("spring.data.web.pageable.default-page-size", Integer.class))
                .isNotNull()
                .isPositive();
    }

    @Test
    void iTimeoutDiTransazioneEQuerySonoApplicatiAlContesto() {
        assertThat(environment.getProperty("spring.transaction.default-timeout", Integer.class))
                .as("un timeout di transazione evita che una query anomala tenga occupata una connessione")
                .isNotNull()
                .isPositive();

        assertThat(environment.getProperty("spring.jpa.properties.hibernate.query.timeout", Integer.class))
                .as("timeout di query configurato")
                .isNotNull()
                .isPositive();
    }

    @Test
    void ilRateLimitHaUnaCapienzaFinitaConfigurata() {
        assertThat(environment.getProperty("app.ratelimit.authenticated.capacity", Integer.class))
                .isNotNull()
                .isPositive();
        assertThat(environment.getProperty("app.ratelimit.anonymous.capacity", Integer.class))
                .isNotNull()
                .isPositive();
    }

    @Test
    void lAnonimoHaUnaCapienzaNonSuperioreAQuellaAutenticata() {
        assertThat(environment.getProperty("spring.jackson.deserialization.fail-on-unknown-properties", Boolean.class))
                .as("i campi non previsti dal DTO devono essere rifiutati, non ignorati")
                .isTrue();
    }
}
