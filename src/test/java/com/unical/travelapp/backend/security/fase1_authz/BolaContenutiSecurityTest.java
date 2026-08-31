package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 1 - BOLA su itinerari, attivita' e recensioni.
 *
 * <p>Un organizzatore non deve poter cancellare il catalogo di un altro organizzatore, e
 * l'autore di una recensione e' sempre quello del token, mai un id passato nel payload.
 */
class BolaContenutiSecurityTest extends SecurityIntegrationTestBase {

    private static final String SUB_ORGANIZZATORE_2 = "sub-organizzatore-2";

    private Utente organizzatore1;
    private Utente organizzatore2;
    private Utente viaggiatoreA;
    private Itinerario itinerarioDi1;
    private Recensione recensioneDiB;

    @BeforeEach
    void catalogoDiDueOrganizzatori() {
        organizzatore1 = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        organizzatore2 = utente(SUB_ORGANIZZATORE_2, Ruolo.ORGANIZZATORE);
        viaggiatoreA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        Utente viaggiatoreB = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);

        itinerarioDi1 = itinerario(organizzatore1);
        recensioneDiB = recensione(viaggiatoreB, itinerarioDi1);
    }

    @Test
    void unOrganizzatoreNonCancellaLItinerarioDiUnAltro() throws Exception {
        MvcResult risultato = mockMvc.perform(delete("/api/itinerari/" + itinerarioDi1.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE_2, "ORGANIZZATORE")))
                .andExpect(status().isNotFound())
                .andReturn();
        NessunLeak.verifica(risultato);

        assertThat(itinerarioRepository.existsById(itinerarioDi1.getId()))
                .as("l'itinerario dell'organizzatore 1 deve esistere ancora")
                .isTrue();
    }

    @Test
    void ilProprietarioCancellaIlProprioItinerario() throws Exception {
        mockMvc.perform(delete("/api/itinerari/" + itinerarioDi1.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isNoContent());

        assertThat(itinerarioRepository.existsById(itinerarioDi1.getId())).isFalse();
    }

    @Test
    void lAdminCancellaQualsiasiItinerario() throws Exception {
        mockMvc.perform(delete("/api/itinerari/" + itinerarioDi1.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isNoContent());

        assertThat(itinerarioRepository.existsById(itinerarioDi1.getId())).isFalse();
    }

    @Test
    void itinerarioAltruiEItinerarioInesistenteSonoIndistinguibili() throws Exception {
        int altrui = mockMvc.perform(delete("/api/itinerari/" + itinerarioDi1.getId())
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE_2, "ORGANIZZATORE"))).andReturn().getResponse().getStatus();
        int inesistente = mockMvc.perform(delete("/api/itinerari/999999")
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE_2, "ORGANIZZATORE"))).andReturn().getResponse().getStatus();

        assertThat(altrui).isEqualTo(inesistente).isEqualTo(404);
    }

    @Test
    void unOrganizzatoreNonCancellaLAttivitaDiUnAltro() throws Exception {
        SingolaAttivita attivitaDi1 = new SingolaAttivita();
        attivitaDi1.setOrganizzatore(organizzatore1);
        attivitaDi1.setTitolo("Rafting");
        attivitaDi1.setLuogo("Lao");
        attivitaDi1.setPrezzo(new BigDecimal("40.00"));
        attivitaDi1.setMaxPartecipanti(10);
        SingolaAttivita salvata = singolaAttivitaRepository.save(attivitaDi1);

        mockMvc.perform(delete("/api/attivita/" + salvata.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE_2, "ORGANIZZATORE")))
                .andExpect(status().isNotFound());

        assertThat(singolaAttivitaRepository.existsById(salvata.getId())).isTrue();
    }

    @Test
    void lItinerarioCreatoEIntestatoAllOrganizzatoreDelToken() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE_2, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Nuovo\",\"destinazionePrincipale\":\"Scilla\","
                                + "\"prezzoBase\":50.0,\"durataGiorni\":1,\"maxPartecipanti\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizzatoreId").value(organizzatore2.getId().intValue()));

        assertThat(itinerarioRepository.findAll())
                .filteredOn(i -> "Nuovo".equals(i.getTitolo()))
                .singleElement()
                .satisfies(i -> {
                    assertThat(i.getOrganizzatore().getId()).isEqualTo(organizzatore2.getId());
                    assertThat(i.getStato())
                            .as("lo stato iniziale lo decide il server, non il client")
                            .isEqualTo("BOZZA");
                });
    }

    @Test
    void nonSiPuoIntestareUnItinerarioAUnAltroOrganizzatoreDalPayload() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE_2, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Furto\",\"destinazionePrincipale\":\"Scilla\","
                                + "\"prezzoBase\":50.0,\"durataGiorni\":1,\"maxPartecipanti\":8,"
                                + "\"organizzatoreId\":" + organizzatore1.getId() + ",\"stato\":\"PUBBLICATO\"}"))
                .andExpect(status().isBadRequest());

        assertThat(itinerarioRepository.findAll())
                .noneMatch(i -> "Furto".equals(i.getTitolo()));
    }

    @Test
    void soloLAutoreCancellaLaPropriaRecensione() throws Exception {
        MvcResult risultato = mockMvc.perform(delete("/api/recensioni/" + recensioneDiB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isForbidden())
                .andReturn();
        NessunLeak.verifica(risultato);

        assertThat(recensioneRepository.existsById(recensioneDiB.getId()))
                .as("la recensione di B deve esistere ancora")
                .isTrue();

        mockMvc.perform(delete("/api/recensioni/" + recensioneDiB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk());
        assertThat(recensioneRepository.existsById(recensioneDiB.getId())).isFalse();
    }

    @Test
    void laRecensioneCreataEIntestataAllUtenteDelToken() throws Exception {
        // si recensisce una prenotazione conclusa, non un itinerario qualsiasi del catalogo
        var prenotazioneDiA = prenotazioneConclusa(viaggiatoreA, itinerarioDi1);

        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazioneDiA.getId()
                                + ",\"votazione\":5,\"comm\":\"ottimo\"}"))
                .andExpect(status().isCreated());

        assertThat(recensioneRepository.findAll())
                .filteredOn(r -> "ottimo".equals(r.getCommento()))
                .singleElement()
                .satisfies(r -> assertThat(r.getUtente().getId()).isEqualTo(viaggiatoreA.getId()));
    }

    @Test
    void nonSiPuoRecensireUnItinerarioSenzaAverloPrenotato() throws Exception {
        // senza prenotazioneId non c'e' modo di dimostrare di aver fatto quel viaggio
        MvcResult risultato = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":" + itinerarioDi1.getId()
                                + ",\"votazione\":5,\"comm\":\"mai stato li\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        NessunLeak.verifica(risultato);

        assertThat(recensioneRepository.findAll()).noneMatch(r -> "mai stato li".equals(r.getCommento()));
    }

    @Test
    void nonSiPuoIntestareUnaRecensioneAUnAltroUtenteDalPayload() throws Exception {
        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":" + itinerarioDi1.getId()
                                + ",\"votazione\":5,\"comm\":\"falso\",\"utenteId\":999,\"autoreId\":999}"))
                .andExpect(status().isBadRequest());

        assertThat(recensioneRepository.findAll()).noneMatch(r -> "falso".equals(r.getCommento()));
    }

    @Test
    void nonSiPuoRecensireLaPrenotazioneDiUnAltroUtente() throws Exception {
        Utente viaggiatoreB = utenteRepository.findByKeycloakId(SUB_UTENTE_B).orElseThrow();
        var prenotazioneDiB = prenotazioneConclusa(viaggiatoreB, itinerarioDi1);

        MvcResult risultato = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazioneDiB.getId()
                                + ",\"itinerarioId\":" + itinerarioDi1.getId()
                                + ",\"votazione\":1,\"comm\":\"sabotaggio\"}"))
                .andExpect(status().isForbidden())
                .andReturn();
        NessunLeak.verifica(risultato);

        assertThat(recensioneRepository.findAll()).noneMatch(r -> "sabotaggio".equals(r.getCommento()));
    }
}
