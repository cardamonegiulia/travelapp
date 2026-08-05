package com.unical.travelapp.backend.security.fase2_io;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 2 - excessive data exposure (OWASP API3:2023).
 *
 * <p>Le asserzioni sono sul JSON prodotto, non sull'oggetto Java: e' quello che il client
 * riceve davvero. I DTO di risposta non devono contenere campi interni ne' dati di utenti
 * diversi dal richiedente.
 */
class NessunDatoSensibileNelleRisposteTest extends SecurityIntegrationTestBase {

    private static final List<String> CAMPI_MAI_ESPOSTI = List.of(
            "password", "passwd", "hash", "secret", "segreto", "clientsecret",
            "token", "authorization", "bearer", "keycloakid",
            "creatoda", "modificatoda", "creatoil", "modificatoil");

    private Utente utenteA;
    private Utente utenteB;
    private Itinerario itinerarioPubblico;
    private DisponibilitaItinerario disponibilita;

    @BeforeEach
    void dati() {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utenteA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utenteB = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
        itinerarioPubblico = itinerario(organizzatore);
        disponibilita = disponibilita(itinerarioPubblico, 10);
    }

    private void nessunCampoSensibile(MvcResult risultato) throws Exception {
        String corpo = risultato.getResponse().getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        for (String campo : CAMPI_MAI_ESPOSTI) {
            assertThat(corpo)
                    .as("il campo \"%s\" non deve comparire nella risposta", campo)
                    .doesNotContain("\"" + campo + "\"");
        }
    }

    @Test
    void ilProfiloUtenteNonEsponeIdentificativiInterniNeCampiDiAudit() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/utenti/" + utenteA.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andReturn();

        nessunCampoSensibile(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .as("il keycloakId non e' un dato da restituire al client")
                .doesNotContain(SUB_UTENTE_A);
    }

    @Test
    void lElencoUtentiPerLAdminNonEsponeIdentificativiKeycloak() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/utenti")
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        nessunCampoSensibile(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .doesNotContain(SUB_UTENTE_A)
                .doesNotContain(SUB_UTENTE_B);
    }

    @Test
    void laPrenotazioneNonEsponeLEmailDiAltriUtenti() throws Exception {
        var prenotazioneDiA = prenotazione(utenteA, disponibilita);

        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/" + prenotazioneDiA.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andReturn();

        nessunCampoSensibile(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .as("nessuna email nel DTO di prenotazione")
                .doesNotContain(utenteA.getEmail())
                .doesNotContain(utenteB.getEmail());
    }

    @Test
    void lItinerarioNonEsponeLAnagraficaCompletaDellOrganizzatore() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari/" + itinerarioPubblico.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andReturn();

        nessunCampoSensibile(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .as("dell'organizzatore deve uscire solo l'id, non l'email")
                .doesNotContain("@example.test");
    }

    @Test
    void laRecensioneNonEsponeLAnagraficaDellAutore() throws Exception {
        var recensione = recensione(utenteB, itinerarioPubblico);

        MvcResult risultato = mockMvc.perform(get("/api/recensioni/" + recensione.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andReturn();

        nessunCampoSensibile(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .doesNotContain(utenteB.getEmail())
                .doesNotContain(SUB_UTENTE_B);
    }

    @Test
    void lElencoDelleRecensioniDiUnItinerarioNonEsponeLeEmailDegliAutori() throws Exception {
        recensione(utenteA, itinerarioPubblico);
        recensione(utenteB, itinerarioPubblico);

        MvcResult risultato = mockMvc.perform(
                        get("/api/recensioni/itinerario/" + itinerarioPubblico.getId())
                                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andReturn();

        nessunCampoSensibile(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .doesNotContain("@example.test");
    }

    @Test
    void laSincronizzazioneUtenteNonRestituisceIlContenutoDelToken() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andReturn();

        nessunCampoSensibile(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .as("il DTO di risposta non deve contenere claim o identificativi del token")
                .doesNotContain(SUB_UTENTE_A)
                .doesNotContain("realm_access")
                .doesNotContain("resource_access");
    }
}
