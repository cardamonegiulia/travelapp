package com.unical.travelapp.backend.security.fase6_rete;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class HeaderDiSicurezzaSecurityTest extends SecurityIntegrationTestBase {

    @BeforeEach
    void utenti() {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        itinerario(utenteRepository.findByKeycloakId(SUB_ORGANIZZATORE).orElseThrow());
    }

    private void verificaHeader(MvcResult risultato) {
        assertThat(risultato.getResponse().getHeader("X-Frame-Options"))
                .as("difesa contro il clickjacking")
                .isEqualTo("DENY");
        assertThat(risultato.getResponse().getHeader("X-Content-Type-Options"))
                .as("il browser non deve indovinare il tipo di contenuto")
                .isEqualTo("nosniff");
        assertThat(risultato.getResponse().getHeader("Referrer-Policy"))
                .isEqualTo("strict-origin-when-cross-origin");

        String csp = risultato.getResponse().getHeader("Content-Security-Policy");
        assertThat(csp).isNotNull();
        assertThat(csp)
                .contains("default-src 'self'")
                .contains("frame-ancestors 'none'")
                .contains("object-src 'none'");
    }

    @Test
    void gliHeaderSonoPresentiSuUnaRisposta200() throws Exception {
        verificaHeader(mockMvc.perform(get("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn());
    }

    @Test
    void gliHeaderSonoPresentiSuUn401() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")).andReturn();
        assertThat(risultato.getResponse().getStatus()).isEqualTo(401);
        verificaHeader(risultato);
    }

    @Test
    void gliHeaderSonoPresentiSuUn403() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/utenti")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();
        assertThat(risultato.getResponse().getStatus()).isEqualTo(403);
        verificaHeader(risultato);
    }

    @Test
    void gliHeaderSonoPresentiSuUn404() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/999999")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();
        assertThat(risultato.getResponse().getStatus()).isEqualTo(404);
        verificaHeader(risultato);
    }

    @Test
    void gliHeaderSonoPresentiSuUn400() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/non-un-numero")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();
        assertThat(risultato.getResponse().getStatus()).isEqualTo(400);
        verificaHeader(risultato);
    }

    @Test
    void laCspVietaLaFramificazioneEGliOggettiEsterni() throws Exception {
        String csp = mockMvc.perform(get("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andReturn().getResponse().getHeader("Content-Security-Policy");

        assertThat(csp)
                .as("nessuna origine jolly nella CSP")
                .doesNotContain("default-src *")
                .doesNotContain("script-src *");
    }

    @Test
    void suHttpNonVieneEmessoHstsInSviluppo() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getResponse().getHeader("Strict-Transport-Security"))
                .as("nessun HSTS su richiesta non cifrata")
                .isNull();
    }

    @Test
    void suHttpsVieneEmessoHstsConMaxAgeESubdomini() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")
                        .secure(true)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andReturn();

        assertThat(risultato.getResponse().getHeader("Strict-Transport-Security"))
                .as("su canale sicuro HSTS deve esserci")
                .isNotNull()
                .contains("max-age=31536000")
                .contains("includeSubDomains");
    }
}
