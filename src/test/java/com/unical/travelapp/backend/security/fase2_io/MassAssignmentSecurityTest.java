package com.unical.travelapp.backend.security.fase2_io;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 2 - mass assignment (OWASP API6:2023, Broken Object Property Level Authorization).
 *
 * <p>{@code spring.jackson.deserialization.fail-on-unknown-properties=true}: un campo di
 * sistema iniettato nel payload deve far fallire la richiesta con 400, non essere ignorato
 * in silenzio (che nasconderebbe il tentativo) ne' tantomeno applicato.
 */
class MassAssignmentSecurityTest extends SecurityIntegrationTestBase {

    private Utente organizzatore;
    private Itinerario itinerarioEsistente;
    private DisponibilitaItinerario disponibilita;

    @BeforeEach
    void datiDiBase() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
        itinerarioEsistente = itinerario(organizzatore);
        disponibilita = disponibilita(itinerarioEsistente, 10);
    }

    @ParameterizedTest(name = "campo di sistema nell''itinerario: {0}")
    @ValueSource(strings = {"id", "organizzatoreId", "stato", "creatoDa", "creatoIl", "modificatoDa", "recensioni"})
    void iCampiDiSistemaNelPayloadItinerarioFannoFallireLaRichiesta(String campo) throws Exception {
        String valore = switch (campo) {
            case "id", "organizzatoreId" -> "99";
            case "recensioni" -> "[]";
            default -> "\"PUBBLICATO\"";
        };

        MvcResult risultato = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Tentativo\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"durataGiorni\":1,\"maxPartecipanti\":2,\"" + campo + "\":" + valore + "}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(itinerarioRepository.findAll())
                .as("il payload con campo di sistema non deve creare nulla")
                .noneMatch(i -> "Tentativo".equals(i.getTitolo()));
    }

    @ParameterizedTest(name = "campo di sistema nella prenotazione: {0}")
    @ValueSource(strings = {"id", "viaggiatore", "viaggiatoreId", "utenteId", "stato", "prezzoTotale", "creatoDa"})
    void iCampiDiSistemaNelPayloadPrenotazioneFannoFallireLaRichiesta(String campo) throws Exception {
        String valore = campo.equals("stato") ? "\"CONFERMATA\"" : "99";

        mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + disponibilita.getId()
                                + ",\"numeroPartecipanti\":1,\"" + campo + "\":" + valore + "}"))
                .andExpect(status().isBadRequest());

        assertThat(prenotazioneRepository.findAll()).isEmpty();
    }

    @Test
    void nonSiPuoForzareIlPrezzoDiUnaPrenotazione() throws Exception {
        // il prezzo lo calcola il server dal listino, non lo decide il client
        mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + disponibilita.getId()
                                + ",\"numeroPartecipanti\":2,\"prezzoTotale\":0.01}"))
                .andExpect(status().isBadRequest());

        // la stessa richiesta senza il campo estraneo passa e il prezzo lo mette il server
        mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + disponibilita.getId()
                                + ",\"numeroPartecipanti\":2}"))
                .andExpect(status().isCreated());

        assertThat(prenotazioneRepository.findAll())
                .singleElement()
                .satisfies(p -> assertThat(p.getPrezzoTotale())
                        .as("prezzo calcolato dal server: 2 partecipanti x 100.00")
                        .isEqualByComparingTo("200.00"));
    }

    @Test
    void nonSiPuoForzareIlRuoloAllaCreazioneDiUnUtente() throws Exception {
        // "ruolo" e' un campo legittimo di UtenteDto, ma l'endpoint e' riservato agli ADMIN:
        // il controllo qui e' l'autorizzazione, non la deserializzazione
        mockMvc.perform(post("/api/utenti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keycloakId\":\"k\",\"nome\":\"Mal\",\"cognome\":\"Intenzionato\","
                                + "\"email\":\"m@example.test\",\"ruolo\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        assertThat(utenteRepository.findByKeycloakId("k")).isEmpty();
    }

    @ParameterizedTest(name = "campo di audit nel payload utente: {0}")
    @ValueSource(strings = {"creatoDa", "creatoIl", "modificatoDa", "modificatoIl", "id", "keycloakId"})
    void iCampiDiAuditNonSonoAccettatiInAggiornamento(String campo) throws Exception {
        Utente utenteA = utenteRepository.findByKeycloakId(SUB_UTENTE_A).orElseThrow();
        String valore = campo.equals("id") ? "99" : "\"valore-iniettato\"";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/utenti/" + utenteA.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ada\",\"" + campo + "\":" + valore + "}"))
                .andExpect(status().isBadRequest());

        Utente dopo = utenteRepository.findById(utenteA.getId()).orElseThrow();
        assertThat(dopo.getCreatoDa()).isNotEqualTo("valore-iniettato");
        assertThat(dopo.getModificatoDa()).isNotEqualTo("valore-iniettato");
        assertThat(dopo.getKeycloakId()).isEqualTo(SUB_UTENTE_A);
    }

    @Test
    void ilCampoSconosciutoVieneRifiutatoAnchePerILPayloadDeiPreferiti() throws Exception {
        mockMvc.perform(post("/api/preferiti/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":" + itinerarioEsistente.getId() + ",\"utenteId\":99}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ilProprietarioDiUnaListaDiPreferitiNonSiSceglieDalPayload() throws Exception {
        mockMvc.perform(post("/api/preferiti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Lista\",\"utenteId\":99,\"proprietarioId\":99}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unPayloadAnnidatoConCampiEstraneiVieneRifiutato() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"durataGiorni\":1,\"maxPartecipanti\":2,"
                                + "\"organizzatore\":{\"id\":99,\"ruolo\":\"ADMIN\"}}"))
                .andExpect(status().isBadRequest());
    }
}
