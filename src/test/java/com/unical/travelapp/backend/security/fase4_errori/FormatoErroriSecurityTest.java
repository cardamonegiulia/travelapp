package com.unical.travelapp.backend.security.fase4_errori;

import com.fasterxml.jackson.databind.JsonNode;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

class FormatoErroriSecurityTest extends SecurityIntegrationTestBase {

    private Utente utenteA;
    private Utente utenteB;
    private Itinerario itinerario;
    private DisponibilitaItinerario disponibilita;

    @BeforeEach
    void dati() {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utenteA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utenteB = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
        itinerario = itinerario(organizzatore);
        disponibilita = disponibilita(itinerario, 5);
    }

    private JsonNode erroreConforme(MockHttpServletRequestBuilder richiesta, int statusAtteso) throws Exception {
        MvcResult risultato = mockMvc.perform(richiesta).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(statusAtteso);
        NessunLeak.verifica(risultato);

        assertThat(risultato.getResponse().getContentType())
                .as("le risposte di errore devono usare il media type dei ProblemDetail")
                .contains("application/problem+json");

        JsonNode corpo = objectMapper.readTree(risultato.getResponse().getContentAsString());
        assertThat(corpo.get("status").asInt()).isEqualTo(statusAtteso);
        assertThat(corpo.hasNonNull("title")).as("title obbligatorio").isTrue();
        assertThat(corpo.hasNonNull("detail")).as("detail obbligatorio").isTrue();
        assertThat(corpo.hasNonNull("type")).as("type obbligatorio").isTrue();
        assertThat(corpo.get("type").asText()).startsWith("urn:travelapp:problem:");
        assertThat(corpo.hasNonNull("instance")).as("instance obbligatorio").isTrue();
        assertThat(corpo.hasNonNull("traceId")).as("traceId obbligatorio per correlare i log").isTrue();
        return corpo;
    }

    @Test
    void errore400ValidazioneFallita() throws Exception {
        erroreConforme(post("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"\"}"), 400);
    }

    @Test
    void errore400PayloadNonLeggibile() throws Exception {
        erroreConforme(post("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{non-json}"), 400);
    }

    @Test
    void errore400ParametroNonConvertibile() throws Exception {
        erroreConforme(get("/api/prenotazioni/non-un-numero")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")), 400);
    }

    @Test
    void errore400OrdinamentoSuCampoInesistente() throws Exception {
        erroreConforme(get("/api/itinerari").param("sort", "campoInventato,asc")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")), 400);
    }

    @Test
    void errore401SenzaToken() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(401);
        NessunLeak.verifica(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .as("il 401 non deve spiegare perche' il token non va bene")
                .doesNotContain("jwt")
                .doesNotContain("signature");
    }

    @Test
    void errore403RuoloInsufficiente() throws Exception {
        erroreConforme(get("/api/utenti")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")), 403);
    }

    @Test
    void errore404RisorsaNonTrovata() throws Exception {
        erroreConforme(get("/api/prenotazioni/999999")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")), 404);
    }

    @Test
    void errore404RottaNonMappata() throws Exception {
        erroreConforme(get("/api/questa-rotta-non-esiste")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")), 404);
    }

    @Test
    void errore405MetodoNonAmmesso() throws Exception {
        erroreConforme(patch("/api/itinerari/" + itinerario.getId())
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")), 405);
    }

    @Test
    void errore409StatoNonValido() throws Exception {
        var prenotazione = prenotazione(utenteA, disponibilita);
        mockMvc.perform(post("/api/prenotazioni/" + prenotazione.getId() + "/annulla")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        erroreConforme(post("/api/prenotazioni/" + prenotazione.getId() + "/annulla")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")), 409);
    }

    @Test
    void errore409RisorsaGiaEsistente() throws Exception {
        erroreConforme(post("/api/utenti")
                .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keycloakId\":\"" + SUB_UTENTE_A + "\",\"nome\":\"Doppio\","
                        + "\"cognome\":\"Utente\",\"email\":\"" + utenteA.getEmail() + "\"}"), 409);
    }

    @Test
    void errore415ContentTypeNonSupportato() throws Exception {
        erroreConforme(post("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                .contentType(MediaType.TEXT_PLAIN)
                .content("testo semplice"), 415);
    }

    @Test
    void ilConflittoDiIntegritaNonEsponeIlVincoloViolato() throws Exception {
        JsonNode corpo = erroreConforme(post("/api/utenti")
                .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keycloakId\":\"nuovo-sub\",\"nome\":\"Doppio\","
                        + "\"cognome\":\"Utente\",\"email\":\"" + utenteB.getEmail() + "\"}"), 409);

        assertThat(corpo.get("detail").asText().toLowerCase())
                .as("mai il nome del vincolo o della colonna nel body")
                .doesNotContain("uk_")
                .doesNotContain("unique")
                .doesNotContain("utenti");
    }

    @Test
    void tutteLeRisposteDiErroreDellaSuitePassanoIlControlloAntiLeak() throws Exception {
        List<MockHttpServletRequestBuilder> richieste = new ArrayList<>(List.of(
                get("/api/itinerari"),
                get("/api/utenti").with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")),
                get("/api/prenotazioni/999999").with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")),
                get("/api/prenotazioni/xyz").with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")),
                get("/api/recensioni/999999").with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")),
                delete("/api/itinerari/999999").with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")),
                patch("/api/utenti/1").with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")),
                request(HttpMethod.PUT, "/api/itinerari").with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")),
                post("/api/itinerari").with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"),
                post("/api/recensioni").with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"votazione\":99}"),
                get("/api/itinerari").param("sort", "xxx,asc")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")),
                get("/api/utenti/" + utenteB.getId()).with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
        ));

        int erroriVerificati = 0;
        for (MockHttpServletRequestBuilder richiesta : richieste) {
            MvcResult risultato = mockMvc.perform(richiesta).andReturn();
            if (risultato.getResponse().getStatus() >= 400) {
                NessunLeak.verifica(risultato);
                erroriVerificati++;
            }
        }

        assertThat(erroriVerificati)
                .as("la batteria deve aver prodotto errori veri da controllare")
                .isEqualTo(richieste.size());
    }

    @Test
    void ilFallbackA500NonCambiaComportamentoInBaseAllInput() throws Exception {
        MvcResult primo = mockMvc.perform(get("/api/itinerari").param("sort", "a; DROP TABLE utenti")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();
        MvcResult secondo = mockMvc.perform(get("/api/itinerari").param("sort", "b; SELECT 1")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(primo.getResponse().getStatus()).isEqualTo(secondo.getResponse().getStatus());
        assertThat(senzaTraceId(primo)).isEqualTo(senzaTraceId(secondo));
        NessunLeak.verifica(primo);
        NessunLeak.verifica(secondo);
    }

    private String senzaTraceId(MvcResult risultato) throws Exception {
        return risultato.getResponse().getContentAsString()
                .replaceAll("\"traceId\":\"[^\"]*\"", "");
    }
}
