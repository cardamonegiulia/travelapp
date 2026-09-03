package com.unical.travelapp.backend.security.fase6_rete;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class StatelessECsrfSecurityTest extends SecurityIntegrationTestBase {

    private Utente utenteA;

    @BeforeEach
    void utenti() {
        utenteA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
    }

    @Test
    void nessunaRichiestaAutenticataEmetteUnCookieDiSessione() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getResponse().getCookies())
                .as("API stateless: nessun cookie deve essere emesso")
                .isEmpty();
        assertThat(risultato.getResponse().getHeader("Set-Cookie")).isNull();
    }

    @Test
    void nessunaSessioneHttpVieneCreata() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getRequest().getSession(false))
                .as("SessionCreationPolicy.STATELESS: nessuna sessione lato server")
                .isNull();
    }

    @Test
    void nemmenoUnaRichiestaRespintaCreaUnaSessione() throws Exception {
        MvcResult senzaToken = mockMvc.perform(get("/api/itinerari")).andReturn();
        MvcResult negata = mockMvc.perform(get("/api/utenti")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(senzaToken.getRequest().getSession(false)).isNull();
        assertThat(negata.getRequest().getSession(false)).isNull();
        assertThat(senzaToken.getResponse().getCookies()).isEmpty();
        assertThat(negata.getResponse().getCookies()).isEmpty();
    }

    @Test
    void unCookieDiSessioneFornitoDalClientNonAutentica() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")
                        .cookie(new Cookie("JSESSIONID", "una-sessione-inventata")))
                .andReturn();

        assertThat(risultato.getResponse().getStatus())
                .as("nessun cookie deve poter sostituire il bearer token")
                .isEqualTo(401);
    }

    @Test
    void dueRichiesteConLoStessoClientNonCondividonoStato() throws Exception {
        mockMvc.perform(get("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        MvcResult seconda = mockMvc.perform(get("/api/itinerari")).andReturn();

        assertThat(seconda.getResponse().getStatus())
                .as("nessuno stato di sessione deve sopravvivere fra le richieste")
                .isEqualTo(401);
    }

    @Test
    void unaScritturaSenzaTokenCsrfMaConBearerValidoFunziona() throws Exception {
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andReturn();

        MvcResult risultato = mockMvc.perform(post("/api/utenti/me")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void unaScritturaSenzaAlcunaCredenzialeVieneRespinta() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/itinerari")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"Da un altro sito\",\"destinazionePrincipale\":\"D\","
                                + "\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}"))
                .andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(401);
        assertThat(itinerarioRepository.findAll())
                .as("una richiesta cross-site senza token non deve creare nulla")
                .isEmpty();
    }

    @Test
    void unaRichiestaCrossOriginConCookieMaSenzaBearerNonPassa() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/utenti/" + utenteA.getId())
                        .header("Origin", "https://sito-malevolo.example")
                        .cookie(new Cookie("JSESSIONID", "sessione-rubata"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Compromesso\"}"))
                .andReturn();

        assertThat(risultato.getResponse().getStatus()).isIn(401, 403, 405);
        assertThat(utenteRepository.findById(utenteA.getId()).orElseThrow().getNome())
                .isEqualTo(utenteA.getNome());
    }
}
