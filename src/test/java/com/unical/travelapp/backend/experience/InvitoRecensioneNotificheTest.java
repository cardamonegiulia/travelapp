package com.unical.travelapp.backend.experience;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.models.TipoNotifica;
import com.unical.travelapp.backend.experience.services.InvitoRecensioneJob;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Notifica di invito a recensire")
class InvitoRecensioneNotificheTest extends SecurityIntegrationTestBase {

    @Autowired
    private InvitoRecensioneJob job;

    private Utente viaggiatore;
    private Itinerario itinerario;
    private LocalDate ieri;

    @BeforeEach
    void datiDiPartenza() {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        viaggiatore = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        itinerario = itinerario(organizzatore);
        ieri = LocalDate.now().minusDays(1);
    }

    private Prenotazione viaggioFinitoIeri(Utente chi) {
        DisponibilitaItinerario partenza = new DisponibilitaItinerario();
        partenza.setItinerario(itinerario);
        partenza.setDataInizio(ieri.minusDays(4).atStartOfDay());
        partenza.setDataFine(ieri.atTime(12, 0));
        partenza.setPostiDisponibili(10);
        return prenotazione(chi, disponibilitaRepository.save(partenza));
    }

    @Test
    void ilGiornoDopoLaFineDelViaggioArrivaLInvitoARecensire() throws Exception {
        Prenotazione prenotazione = viaggioFinitoIeri(viaggiatore);

        assertThat(job.generaInvitiPerViaggiConclusiIl(ieri)).isEqualTo(1);

        assertThat(notificaRepository.findAll())
                .singleElement()
                .satisfies(notifica -> {
                    assertThat(notifica.getTipo()).isEqualTo(TipoNotifica.INVITO_RECENSIONE);
                    assertThat(notifica.getDestinatario().getId()).isEqualTo(viaggiatore.getId());
                    assertThat(notifica.getPrenotazione().getId()).isEqualTo(prenotazione.getId());
                    assertThat(notifica.isLetta()).isFalse();
                });

        mockMvc.perform(get("/api/notifiche")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("INVITO_RECENSIONE"))
                .andExpect(jsonPath("$.content[0].prenotazioneId").value(prenotazione.getId().intValue()))
                .andExpect(jsonPath("$.content[0].itinerarioId").value(itinerario.getId().intValue()));

        mockMvc.perform(get("/api/notifiche/non-lette")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    void ilJobEseguitoDueVolteNonDuplicaLaNotifica() {
        viaggioFinitoIeri(viaggiatore);

        assertThat(job.generaInvitiPerViaggiConclusiIl(ieri)).isEqualTo(1);
        assertThat(job.generaInvitiPerViaggiConclusiIl(ieri))
                .as("la seconda esecuzione non deve creare nulla")
                .isZero();

        assertThat(notificaRepository.findAll()).hasSize(1);
    }

    @Test
    void chiHaGiaRecensitoNonVieneInvitato() throws Exception {
        Prenotazione prenotazione = viaggioFinitoIeri(viaggiatore);

        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazione.getId() + ",\"votazione\":5}"))
                .andExpect(status().isCreated());

        assertThat(job.generaInvitiPerViaggiConclusiIl(ieri)).isZero();
        assertThat(notificaRepository.findAll()).isEmpty();
    }

    @Test
    void recensireFaSparireLInvitoGiaRicevuto() throws Exception {
        Prenotazione prenotazione = viaggioFinitoIeri(viaggiatore);
        job.generaInvitiPerViaggiConclusiIl(ieri);
        assertThat(notificaRepository.findAll()).hasSize(1);

        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + prenotazione.getId() + ",\"votazione\":4}"))
                .andExpect(status().isCreated());

        assertThat(notificaRepository.findAll())
                .as("un invito a fare una cosa gia' fatta non deve restare in elenco")
                .isEmpty();
    }

    @Test
    void unaPrenotazioneCancellataNonGeneraInviti() {
        Prenotazione prenotazione = viaggioFinitoIeri(viaggiatore);
        prenotazione.setStato(StatoPrenotazione.CANCELLATA);
        prenotazioneRepository.save(prenotazione);

        assertThat(job.generaInvitiPerViaggiConclusiIl(ieri)).isZero();
        assertThat(notificaRepository.findAll()).isEmpty();
    }

    @Test
    void iViaggiChiusiInAltreGiornateNonVengonoToccati() {
        prenotazione(viaggiatore, disponibilita(itinerario, 10));
        DisponibilitaItinerario vecchia = new DisponibilitaItinerario();
        vecchia.setItinerario(itinerario);
        vecchia.setDataInizio(LocalDateTime.now().minusDays(12));
        vecchia.setDataFine(LocalDateTime.now().minusDays(7));
        vecchia.setPostiDisponibili(10);
        prenotazione(viaggiatore, disponibilitaRepository.save(vecchia));

        assertThat(job.generaInvitiPerViaggiConclusiIl(ieri)).isZero();
        assertThat(notificaRepository.findAll()).isEmpty();
    }

    @Test
    void leNotificheDiUnUtenteNonSonoLeggibiliDagliAltri() throws Exception {
        viaggioFinitoIeri(viaggiatore);
        job.generaInvitiPerViaggiConclusiIl(ieri);

        long idNotifica = notificaRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/notifiche")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        MvcResult risultato = mockMvc.perform(post("/api/notifiche/" + idNotifica + "/letta")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isNotFound())
                .andReturn();
        NessunLeak.verifica(risultato);

        assertThat(notificaRepository.findById(idNotifica).orElseThrow().isLetta()).isFalse();
    }

    @Test
    void ilDestinatarioSegnaLaPropriaNotificaComeLetta() throws Exception {
        viaggioFinitoIeri(viaggiatore);
        job.generaInvitiPerViaggiConclusiIl(ieri);
        long idNotifica = notificaRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/notifiche/" + idNotifica + "/letta")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.letta").value(true));

        mockMvc.perform(get("/api/notifiche/non-lette")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }
}
