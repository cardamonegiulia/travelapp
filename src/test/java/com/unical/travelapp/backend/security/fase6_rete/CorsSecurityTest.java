package com.unical.travelapp.backend.security.fase6_rete;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

class CorsSecurityTest extends SecurityIntegrationTestBase {

    private static final String ORIGINE_AMMESSA = "https://app.travelapp.test";

    @BeforeEach
    void utenti() {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
    }

    @Test
    void ilPreflightDaUnOrigineInAllowListRiceveGliHeaderCorretti() throws Exception {
        MvcResult risultato = mockMvc.perform(options("/api/itinerari")
                        .header("Origin", ORIGINE_AMMESSA)
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(200);
        assertThat(risultato.getResponse().getHeader("Access-Control-Allow-Origin"))
                .isEqualTo(ORIGINE_AMMESSA);
    }

    @ParameterizedTest(name = "origine non ammessa: {0}")
    @ValueSource(strings = {
            "https://sito-malevolo.example",
            "http://app.travelapp.test",
            "https://app.travelapp.test.malevolo.example",
            "https://app-travelapp.test",
            "null"
    })
    void unOrigineFuoriAllowListNonRiceveAlcunHeaderCors(String origine) throws Exception {
        MvcResult risultato = mockMvc.perform(options("/api/itinerari")
                        .header("Origin", origine)
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        assertThat(risultato.getResponse().getHeader("Access-Control-Allow-Origin"))
                .as("l'origine %s non deve essere autorizzata", origine)
                .isNull();
        assertThat(risultato.getResponse().getStatus())
                .as("il preflight di un'origine non ammessa va respinto")
                .isEqualTo(403);
    }

    @Test
    void nonVieneMaiConcessoIlWildcard() throws Exception {
        MvcResult risultato = mockMvc.perform(options("/api/itinerari")
                        .header("Origin", ORIGINE_AMMESSA)
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        assertThat(risultato.getResponse().getHeader("Access-Control-Allow-Origin"))
                .isNotEqualTo("*");
    }

    @Test
    void leCredenzialiCrossOriginNonSonoMaiConsentite() throws Exception {
        MvcResult risultato = mockMvc.perform(options("/api/itinerari")
                        .header("Origin", ORIGINE_AMMESSA)
                        .header("Access-Control-Request-Method", "POST"))
                .andReturn();

        assertThat(risultato.getResponse().getHeader("Access-Control-Allow-Credentials"))
                .as("bearer token, non cookie: nessuna credenziale cross-origin")
                .satisfiesAnyOf(
                        valore -> assertThat(valore).isNull(),
                        valore -> assertThat(valore).isNotEqualTo("true"));
    }

    @Test
    void soloIMetodiDichiaratiSonoConsentiti() throws Exception {
        MvcResult risultato = mockMvc.perform(options("/api/itinerari")
                        .header("Origin", ORIGINE_AMMESSA)
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        String metodi = risultato.getResponse().getHeader("Access-Control-Allow-Methods");
        assertThat(metodi).isNotNull();
        assertThat(metodi)
                .doesNotContain("TRACE")
                .doesNotContain("PATCH")
                .doesNotContain("*");
        assertThat(metodi).contains("GET").contains("POST").contains("PUT").contains("DELETE");
    }

    @Test
    void unMetodoNonDichiaratoVieneRespintoNelPreflight() throws Exception {
        MvcResult risultato = mockMvc.perform(options("/api/itinerari")
                        .header("Origin", ORIGINE_AMMESSA)
                        .header("Access-Control-Request-Method", "TRACE"))
                .andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(403);
        assertThat(risultato.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void soloGliHeaderDichiaratiSonoConsentiti() throws Exception {
        MvcResult risultato = mockMvc.perform(options("/api/itinerari")
                        .header("Origin", ORIGINE_AMMESSA)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andReturn();

        String header = risultato.getResponse().getHeader("Access-Control-Allow-Headers");
        assertThat(header).isNotNull().doesNotContain("*");
    }

    @Test
    void unaRichiestaSempliceDaOrigineNonAmmessaNonRiceveHeaderCors() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")
                        .header("Origin", "https://sito-malevolo.example")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andReturn();

        assertThat(risultato.getResponse().getHeader("Access-Control-Allow-Origin"))
                .as("senza header CORS il browser blocca la lettura della risposta")
                .isNull();
    }

    @Test
    void ilPreflightNonRichiedeAutenticazioneMaNonEspoDati() throws Exception {
        MvcResult risultato = mockMvc.perform(options("/api/utenti")
                        .header("Origin", ORIGINE_AMMESSA)
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        assertThat(risultato.getResponse().getContentAsString())
                .as("nessun dato nel corpo di un preflight")
                .isEmpty();
    }
}
