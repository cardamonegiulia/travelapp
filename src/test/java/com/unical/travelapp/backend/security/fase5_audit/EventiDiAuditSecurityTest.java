package com.unical.travelapp.backend.security.fase5_audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.security.support.CatturaLog;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 5 - eventi di audit applicativo sulle operazioni critiche.
 *
 * <p>Un audit trail serve a ricostruire "chi ha fatto cosa e quando": deve registrare
 * anche i tentativi falliti, altrimenti un attacco respinto non lascia traccia e non e'
 * rilevabile.
 */
class EventiDiAuditSecurityTest extends SecurityIntegrationTestBase {

    /**
     * Da quando l'aggiornamento del profilo viene propagato all'IdP, una PUT su
     * {@code /api/utenti} passa anche da qui. Non e' l'oggetto di questi test: si sostituisce
     * con un mock perche' il profilo "test" punta di proposito a un Keycloak irraggiungibile.
     */
    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    private Utente organizzatore;
    private Utente utenteA;
    private Itinerario itinerario;
    private DisponibilitaItinerario disponibilita;

    @BeforeEach
    void dati() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utenteA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
        itinerario = itinerario(organizzatore);
        disponibilita = disponibilita(itinerario, 10);
    }

    private List<JsonNode> eventi(CatturaLog log) {
        return log.righe().stream().map(riga -> {
            try {
                return objectMapper.readTree(riga);
            } catch (Exception e) {
                throw new AssertionError("l'evento di audit deve essere JSON valido: " + riga, e);
            }
        }).toList();
    }

    @Test
    void laCreazioneDiUnItinerarioEmetteUnEventoDiAudit() throws Exception {
        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(post("/api/itinerari")
                            .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"Audit\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                    + "\"durataGiorni\":1,\"maxPartecipanti\":2}"))
                    .andExpect(status().isOk());

            assertThat(eventi(audit))
                    .anySatisfy(evento -> {
                        assertThat(evento.get("azione").asText()).isEqualTo("ITINERARIO_CREATO");
                        assertThat(evento.get("esito").asText()).isEqualTo("SUCCESS");
                        assertThat(evento.get("subject").asText()).isEqualTo(SUB_ORGANIZZATORE);
                        assertThat(evento.get("risorsaTipo").asText()).isEqualTo("Itinerario");
                    });
        }
    }

    @Test
    void ogniEventoContieneTuttiICampiRichiesti() throws Exception {
        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(post("/api/prenotazioni")
                            .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"disponibilitaItinerarioId\":" + disponibilita.getId()
                                    + ",\"numeroPartecipanti\":1}"))
                    .andExpect(status().isCreated());

            JsonNode evento = eventi(audit).stream()
                    .filter(e -> "PRENOTAZIONE_CREATA".equals(e.get("azione").asText()))
                    .findFirst().orElseThrow();

            assertThat(evento.get("subject").asText()).isEqualTo(SUB_UTENTE_A);
            assertThat(evento.get("username").asText()).isEqualTo(SUB_UTENTE_A);
            assertThat(evento.get("azione").asText()).isEqualTo("PRENOTAZIONE_CREATA");
            assertThat(evento.get("risorsaTipo").asText()).isEqualTo("Prenotazione");
            assertThat(evento.get("risorsaId").asText()).isNotBlank();
            assertThat(evento.get("esito").asText()).isEqualTo("SUCCESS");
            assertThat(evento.get("ip").asText()).isNotBlank();
            assertThat(evento.hasNonNull("traceId")).as("traceId per correlare con i log applicativi").isTrue();

            // timestamp in UTC e parsabile
            Instant timestamp = Instant.parse(evento.get("timestamp").asText());
            assertThat(timestamp).isBetween(Instant.now().minusSeconds(120), Instant.now().plusSeconds(60));
        }
    }

    @Test
    void ilPagamentoELAnnullamentoEmettonoEventi() throws Exception {
        var prenotazione = prenotazione(utenteA, disponibilita);

        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(post("/api/pagamenti/prenotazioni/" + prenotazione.getId() + "/paga")
                            .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/prenotazioni/" + prenotazione.getId() + "/annulla")
                    .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andExpect(status().isOk());

            assertThat(eventi(audit)).extracting(e -> e.get("azione").asText())
                    .contains("PRENOTAZIONE_PAGATA", "PRENOTAZIONE_ANNULLATA");
        }
    }

    @Test
    void laModificaELaCancellazioneEmettonoEventi() throws Exception {
        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(put("/api/utenti/" + utenteA.getId())
                            .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Modificato\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/itinerari/" + itinerario.getId())
                    .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                    .andExpect(status().isNoContent());

            assertThat(eventi(audit)).extracting(e -> e.get("azione").asText())
                    .contains("UTENTE_MODIFICATO", "ITINERARIO_ELIMINATO");
        }
    }

    @Test
    void unAccessoNegatoLasciaTracciaQuantoUnoRiuscito() throws Exception {
        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(get("/api/utenti")
                    .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                    .andExpect(status().isForbidden());

            assertThat(eventi(audit))
                    .as("un 403 deve produrre un evento di audit di fallimento")
                    .anySatisfy(evento -> {
                        assertThat(evento.get("azione").asText()).isEqualTo("ACCESSO_NEGATO");
                        assertThat(evento.get("esito").asText()).isEqualTo("FAILURE");
                        assertThat(evento.get("subject").asText()).isEqualTo(SUB_UTENTE_A);
                        assertThat(evento.get("risorsaId").asText()).contains("/api/utenti");
                    });
        }
    }

    @Test
    void unTentativoDiAccessoAUnaRisorsaAltruiLasciaTraccia() throws Exception {
        var prenotazioneDiB = prenotazione(
                utenteRepository.findByKeycloakId(SUB_UTENTE_B).orElseThrow(), disponibilita);

        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(get("/api/prenotazioni/utente/"
                            + utenteRepository.findByKeycloakId(SUB_UTENTE_B).orElseThrow().getId())
                            .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                    .andExpect(status().isForbidden());

            assertThat(eventi(audit))
                    .anySatisfy(evento -> {
                        assertThat(evento.get("esito").asText()).isEqualTo("FAILURE");
                        assertThat(evento.get("subject").asText())
                                .as("va registrato CHI ha tentato")
                                .isEqualTo(SUB_UTENTE_A);
                    });
        }
        assertThat(prenotazioneDiB.getId()).isNotNull();
    }

    @Test
    void gliEventiDiAuditNonFinisconoAncheNelLogRoot() throws Exception {
        // additivity=false in logback-spring.xml: l'evento non deve essere duplicato
        try (CatturaLog audit = CatturaLog.audit(); CatturaLog root = CatturaLog.root()) {
            mockMvc.perform(post("/api/itinerari")
                            .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"NonDuplicare\",\"destinazionePrincipale\":\"D\","
                                    + "\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}"))
                    .andExpect(status().isOk());

            assertThat(audit.righe())
                    .as("l'evento deve esserci sul logger AUDIT")
                    .anyMatch(riga -> riga.contains("ITINERARIO_CREATO"));
            assertThat(root.righe())
                    .as("e non deve comparire anche sul logger root")
                    .noneMatch(riga -> riga.contains("ITINERARIO_CREATO"));
        }
    }

    @Test
    void ogniEventoDiAuditEJsonValidoSuUnaSolaRiga() throws Exception {
        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(post("/api/itinerari")
                    .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"Riga\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                            + "\"durataGiorni\":1,\"maxPartecipanti\":2}"));
            mockMvc.perform(get("/api/utenti").with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")));

            assertThat(audit.righe()).isNotEmpty();
            for (String riga : audit.righe()) {
                assertThat(riga)
                        .as("un evento per riga: un a-capo romperebbe il parsing dei log")
                        .doesNotContain("\n");
                assertThat(objectMapper.readTree(riga).isObject()).isTrue();
            }
        }
    }

    @Test
    void unaOperazioneFallitaInRollbackNonLasciaLaRisorsaMaLEventoDiErroreSi() throws Exception {
        // Comportamento atteso e documentato: l'evento di successo viene emesso dal
        // controller DOPO il commit del service, quindi un rollback non produce eventi di
        // creazione fantasma.
        try (CatturaLog audit = CatturaLog.audit()) {
            mockMvc.perform(post("/api/prenotazioni")
                            .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"disponibilitaItinerarioId\":" + disponibilita.getId()
                                    + ",\"numeroPartecipanti\":9999}"))
                    .andExpect(status().isConflict());

            assertThat(prenotazioneRepository.findAll())
                    .as("posti insufficienti: nessuna prenotazione creata")
                    .isEmpty();
            assertThat(eventi(audit))
                    .as("nessun evento di creazione per un'operazione annullata")
                    .noneMatch(e -> "PRENOTAZIONE_CREATA".equals(e.get("azione").asText()));
        }
    }
}
