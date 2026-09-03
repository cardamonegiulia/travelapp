package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BolaPrenotazioniSecurityTest extends SecurityIntegrationTestBase {

    private Utente utenteA;
    private Utente utenteB;
    private Prenotazione prenotazioneDiB;
    private DisponibilitaItinerario disponibilita;

    @BeforeEach
    void datiDiDueUtenti() {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utenteA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utenteB = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);

        Itinerario itinerario = itinerario(organizzatore);
        disponibilita = disponibilita(itinerario, 20);
        prenotazioneDiB = prenotazione(utenteB, disponibilita);
    }

    @Test
    void aNonPuoLeggereLaPrenotazioneDiB() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/" + prenotazioneDiB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(risultato.getResponse().getContentAsString())
                .as("il 404 non deve rivelare nulla della prenotazione altrui")
                .doesNotContain(utenteB.getNome())
                .doesNotContain(utenteB.getEmail())
                .doesNotContain("200.00");
        NessunLeak.verifica(risultato);
    }

    @Test
    void ilProprietarioLeggeLaPropriaPrenotazione() throws Exception {
        mockMvc.perform(get("/api/prenotazioni/" + prenotazioneDiB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viaggiatoreId").value(utenteB.getId().intValue()));
    }

    @Test
    void lAdminAccedeAllePrenotazioniAltrui() throws Exception {
        mockMvc.perform(get("/api/prenotazioni/" + prenotazioneDiB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viaggiatoreId").value(utenteB.getId().intValue()));
    }

    @Test
    void unIdAltruiEUnIdInesistenteSonoIndistinguibili() throws Exception {
        MvcResult altrui = mockMvc.perform(get("/api/prenotazioni/" + prenotazioneDiB.getId())
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();
        MvcResult inesistente = mockMvc.perform(get("/api/prenotazioni/999999")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(altrui.getResponse().getStatus())
                .as("stesso status per id altrui e id inesistente: nessuna enumerazione")
                .isEqualTo(inesistente.getResponse().getStatus())
                .isEqualTo(404);

        assertThat(corpoNormalizzato(altrui)).isEqualTo(corpoNormalizzato(inesistente));
    }

    @Test
    void aNonPuoPagareLaPrenotazioneDiB() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/prenotazioni/" + prenotazioneDiB.getId() + "/paga")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isNotFound())
                .andReturn();
        NessunLeak.verifica(risultato);

        assertThat(prenotazioneRepository.findById(prenotazioneDiB.getId()).orElseThrow().getStato())
                .as("lo stato della prenotazione di B non deve essere cambiato")
                .isEqualTo(prenotazioneDiB.getStato());
    }

    @Test
    void aNonPuoAnnullareLaPrenotazioneDiB() throws Exception {
        mockMvc.perform(post("/api/prenotazioni/" + prenotazioneDiB.getId() + "/annulla")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isNotFound());

        assertThat(prenotazioneRepository.findById(prenotazioneDiB.getId()).orElseThrow().getStato())
                .isEqualTo(prenotazioneDiB.getStato());
    }

    @Test
    void aNonPuoElencareLePrenotazioniDiB() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/prenotazioni/utente/" + utenteB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isForbidden())
                .andReturn();
        NessunLeak.verifica(risultato);
    }

    @Test
    void ognunoElencaLeProprieELAdminQuelleDiTutti() throws Exception {
        mockMvc.perform(get("/api/prenotazioni/utente/" + utenteB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/prenotazioni/utente/" + utenteB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void lIdentitaVieneDalSubNonDallUsernameDelToken() throws Exception {
        mockMvc.perform(get("/api/prenotazioni/" + prenotazioneDiB.getId())
                        .with(TestJwt.conUsernameDiverso(SUB_UTENTE_B, "un-altro-nome", "VIAGGIATORE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/prenotazioni/" + prenotazioneDiB.getId())
                        .with(TestJwt.conUsernameDiverso(SUB_UTENTE_A, utenteB.getEmail(), "VIAGGIATORE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonSiPuoIntestareUnaPrenotazioneAUnAltroUtenteDalPayload() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + disponibilita.getId()
                                + ",\"numeroPartecipanti\":1,\"viaggiatoreId\":" + utenteB.getId()
                                + ",\"utenteId\":" + utenteB.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        NessunLeak.verifica(risultato);

        assertThat(prenotazioneRepository.findAll())
                .as("nessuna prenotazione deve essere stata creata")
                .hasSize(1);
    }

    @Test
    void laPrenotazioneCreataRisultaIntestataAllUtenteDelToken() throws Exception {
        mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + disponibilita.getId()
                                + ",\"numeroPartecipanti\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.viaggiatoreId").value(utenteA.getId().intValue()));

        assertThat(prenotazioneRepository.findAll())
                .filteredOn(p -> !p.getId().equals(prenotazioneDiB.getId()))
                .singleElement()
                .satisfies(p -> assertThat(p.getViaggiatore().getId()).isEqualTo(utenteA.getId()));
    }

    private String corpoNormalizzato(MvcResult risultato) throws Exception {
        return risultato.getResponse().getContentAsString()
                .replaceAll("\"traceId\":\"[^\"]*\"", "\"traceId\":\"<id>\"")
                .replaceAll("\\d+", "<n>");
    }
}
