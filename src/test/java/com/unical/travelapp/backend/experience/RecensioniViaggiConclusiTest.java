package com.unical.travelapp.backend.experience;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Recensioni dei viaggi conclusi")
class RecensioniViaggiConclusiTest extends SecurityIntegrationTestBase {

    private Utente viaggiatoreA;
    private Utente viaggiatoreB;
    private Itinerario itinerario;

    @BeforeEach
    void datiDiPartenza() {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        viaggiatoreA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        viaggiatoreB = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
        itinerario = itinerario(organizzatore);
    }

    @Test
    void chiHaViaggiatoPuoRecensireConLeSoleStelle() throws Exception {
        Prenotazione prenotazione = prenotazioneConclusa(viaggiatoreA, itinerario);

        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazione.getId() + ",\"votazione\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.votazione").value(4))
                .andExpect(jsonPath("$.comm").doesNotExist())
                .andExpect(jsonPath("$.itinerarioId").value(itinerario.getId().intValue()));

        assertThat(recensioneRepository.findAll())
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getUtente().getId()).isEqualTo(viaggiatoreA.getId());
                    assertThat(r.getItinerario().getId()).isEqualTo(itinerario.getId());
                    assertThat(r.getCommento()).isNull();
                });
    }

    @Test
    void nonSiPuoRecensireLaPrenotazioneDiUnAltro() throws Exception {
        Prenotazione prenotazioneDiB = prenotazioneConclusa(viaggiatoreB, itinerario);

        MvcResult risultato = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazioneDiB.getId() + ",\"votazione\":1}"))
                .andExpect(status().isForbidden())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(recensioneRepository.findAll()).isEmpty();
    }

    @Test
    void nonSiPuoRecensireUnViaggioNonAncoraConcluso() throws Exception {
        Prenotazione futura = prenotazione(viaggiatoreA, disponibilita(itinerario, 10));

        MvcResult risultato = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + futura.getId() + ",\"votazione\":5}"))
                .andExpect(status().isConflict())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(recensioneRepository.findAll()).isEmpty();
    }

    @Test
    void nonSiPuoRecensireUnaPrenotazioneCancellata() throws Exception {
        Prenotazione prenotazione = prenotazioneConclusa(viaggiatoreA, itinerario);
        prenotazione.setStato(StatoPrenotazione.CANCELLATA);
        prenotazioneRepository.save(prenotazione);

        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazione.getId() + ",\"votazione\":5}"))
                .andExpect(status().isConflict());

        assertThat(recensioneRepository.findAll()).isEmpty();
    }

    @Test
    void laValutazioneEObbligatoria() throws Exception {
        Prenotazione prenotazione = prenotazioneConclusa(viaggiatoreA, itinerario);

        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazione.getId() + ",\"comm\":\"solo testo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.votazione").exists());

        assertThat(recensioneRepository.findAll()).isEmpty();
    }

    @Test
    void unaSolaRecensionePerPrenotazioneMaModificabile() throws Exception {
        Prenotazione prenotazione = prenotazioneConclusa(viaggiatoreA, itinerario);

        MvcResult creazione = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazione.getId()
                                + ",\"votazione\":2,\"comm\":\"cosi' cosi'\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long idRecensione = objectMapper.readTree(creazione.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazione.getId() + ",\"votazione\":5}"))
                .andExpect(status().isConflict());

        assertThat(recensioneRepository.findAll()).hasSize(1);

        mockMvc.perform(put("/api/recensioni/" + idRecensione)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"votazione\":5,\"comm\":\"col senno di poi, ottimo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votazione").value(5));

        assertThat(recensioneRepository.findAll())
                .singleElement()
                .satisfies(r -> assertThat(r.getVoto()).isEqualTo(5));
    }

    @Test
    void nessunoPuoRiscrivereLaRecensioneDiUnAltro() throws Exception {
        Prenotazione prenotazioneDiB = prenotazioneConclusa(viaggiatoreB, itinerario);

        MvcResult creazione = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazioneDiB.getId() + ",\"votazione\":5}"))
                .andExpect(status().isCreated())
                .andReturn();

        long idRecensione = objectMapper.readTree(creazione.getResponse().getContentAsString())
                .get("id").asLong();

        for (String[] chiamante : new String[][]{{SUB_UTENTE_A, "VIAGGIATORE"}, {SUB_ADMIN, "ADMIN"}}) {
            MvcResult risultato = mockMvc.perform(put("/api/recensioni/" + idRecensione)
                            .with(TestJwt.conRuoliRealm(chiamante[0], chiamante[1]))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"votazione\":1,\"comm\":\"manomessa\"}"))
                    .andExpect(status().isForbidden())
                    .andReturn();
            NessunLeak.verifica(risultato);
        }

        assertThat(recensioneRepository.findById(idRecensione).orElseThrow().getVoto()).isEqualTo(5);
    }

    @Test
    void leRecensioniSonoVisibiliATuttiSullItinerario() throws Exception {
        recensione(viaggiatoreB, itinerario);

        mockMvc.perform(get("/api/itinerari/" + itinerario.getId() + "/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].votazione").value(4))
                .andExpect(jsonPath("$.content[0].comm").value("Bel viaggio"))
                .andExpect(jsonPath("$.content[0].autoreNome").value(viaggiatoreB.getNome()))
                .andExpect(jsonPath("$.content[0].dataRecensione").exists());
    }

    @Test
    void senzaRecensioniLaMediaNonEZeroMaAssente() throws Exception {
        mockMvc.perform(get("/api/itinerari/" + itinerario.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaVoti").doesNotExist())
                .andExpect(jsonPath("$.numeroRecensioni").value(0));
    }

    @Test
    void laMediaDelleStelleCompareSulleAnteprimeDellaBacheca() throws Exception {
        recensione(viaggiatoreA, itinerario);
        Utente terzo = utente("sub-utente-c", Ruolo.VIAGGIATORE);
        var recensioneDaTre = recensione(terzo, itinerario);
        recensioneDaTre.setVoto(3);
        recensioneRepository.save(recensioneDaTre);

        mockMvc.perform(get("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].mediaVoti").value(3.5))
                .andExpect(jsonPath("$.content[0].numeroRecensioni").value(2));
    }

    @Test
    void laSchedaViaggiConclusiContieneSoloIViaggiFiniti() throws Exception {
        Prenotazione conclusa = prenotazioneConclusa(viaggiatoreA, itinerario);
        prenotazione(viaggiatoreA, disponibilita(itinerario, 10));

        mockMvc.perform(get("/api/prenotazioni/mie/concluse")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(conclusa.getId().intValue()))
                .andExpect(jsonPath("$.content[0].recensibile").value(true))
                .andExpect(jsonPath("$.content[0].recensioneId").doesNotExist());

        mockMvc.perform(get("/api/prenotazioni/mie/attuali")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].conclusa").value(false));
    }

    @Test
    void unViaggioGiaRecensitoNonRisultaPiuRecensibile() throws Exception {
        Prenotazione prenotazione = prenotazioneConclusa(viaggiatoreA, itinerario);

        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazione.getId() + ",\"votazione\":5}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/prenotazioni/mie/concluse")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recensibile").value(false))
                .andExpect(jsonPath("$.content[0].recensioneId").exists());

        mockMvc.perform(get("/api/recensioni/prenotazione/" + prenotazione.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votazione").value(5));
    }

    @Test
    void laRecensioneDiUnaPrenotazioneAltruiNonSiPuoLeggere() throws Exception {
        Prenotazione prenotazioneDiB = prenotazioneConclusa(viaggiatoreB, itinerario);

        MvcResult risultato = mockMvc.perform(get("/api/recensioni/prenotazione/" + prenotazioneDiB.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isForbidden())
                .andReturn();

        NessunLeak.verifica(risultato);
    }
}
