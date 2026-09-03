package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BolaUtentiSecurityTest extends SecurityIntegrationTestBase {

    private Utente utenteA;
    private Utente utenteB;

    @BeforeEach
    void dueUtenti() {
        utenteA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utenteB = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
    }

    @Test
    void aLeggeSoloIlProprioProfilo() throws Exception {
        mockMvc.perform(get("/api/utenti/" + utenteA.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(utenteA.getId().intValue()));
    }

    @Test
    void aNonLeggeIlProfiloDiB() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/utenti/" + utenteB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(risultato.getResponse().getContentAsString())
                .doesNotContain(utenteB.getEmail())
                .doesNotContain(utenteB.getNome());
        NessunLeak.verifica(risultato);
    }

    @Test
    void aNonModificaIlProfiloDiB() throws Exception {
        mockMvc.perform(put("/api/utenti/" + utenteB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Rubato\"}"))
                .andExpect(status().isForbidden());

        assertThat(utenteRepository.findById(utenteB.getId()).orElseThrow().getNome())
                .as("il profilo di B non deve essere stato toccato")
                .isEqualTo(utenteB.getNome());
    }

    @Test
    void aNonCancellaIlProfiloDiB() throws Exception {
        mockMvc.perform(delete("/api/utenti/" + utenteB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isForbidden());

        assertThat(utenteRepository.existsById(utenteB.getId()))
                .as("B deve esistere ancora")
                .isTrue();
    }

    @Test
    void lAdminAccedeAiProfiliAltrui() throws Exception {
        mockMvc.perform(get("/api/utenti/" + utenteB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/utenti")
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void unViaggiatoreNonElencaTuttiGliUtenti() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/utenti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(risultato.getResponse().getContentAsString())
                .doesNotContain(utenteB.getEmail());
    }

    @Test
    void unOrganizzatoreNonElencaTuttiGliUtenti() throws Exception {
        mockMvc.perform(get("/api/utenti")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void idAltruiEIdInesistenteDannoLoStessoStatus() throws Exception {
        int altrui = mockMvc.perform(get("/api/utenti/" + utenteB.getId())
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn().getResponse().getStatus();
        int inesistente = mockMvc.perform(get("/api/utenti/999999")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn().getResponse().getStatus();

        assertThat(altrui).isEqualTo(inesistente).isEqualTo(403);
    }

    @Test
    void nonSiPuoCambiareRuoloDaSeStessiTramitePut() throws Exception {
        mockMvc.perform(put("/api/utenti/" + utenteA.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ada\",\"ruolo\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());

        assertThat(utenteRepository.findById(utenteA.getId()).orElseThrow().getRuolo())
                .as("il ruolo non deve essere cambiato")
                .isEqualTo(Ruolo.VIAGGIATORE);
    }

    @Test
    void lIdentitaDiSincronizzazioneVieneDalSubDelToken() throws Exception {
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conUsernameDiverso(SUB_UTENTE_A, "nome-fasullo", "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(utenteA.getId().intValue()));
    }

    @Test
    void unUtenteNonPuoCrearsiUnAccountConRuoloArbitrario() throws Exception {
        mockMvc.perform(post("/api/utenti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keycloakId\":\"nuovo\",\"nome\":\"Mal\",\"cognome\":\"Intenzionato\","
                                + "\"email\":\"mal@example.test\",\"ruolo\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        assertThat(utenteRepository.findByKeycloakId("nuovo")).isEmpty();
    }
}
