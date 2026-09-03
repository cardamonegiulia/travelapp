package com.unical.travelapp.backend.catalog;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PeriodoItinerarioTest extends SecurityIntegrationTestBase {

    private Utente organizzatore;

    @BeforeEach
    void datiDiBase() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
    }

    private String payload(String dataInizio, String dataFine) {
        return "{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"Tour della Sila\",\"destinazionePrincipale\":\"Camigliatello\","
                + "\"prezzoBase\":149.90,\"maxPartecipanti\":15,"
                + "\"dataInizio\":\"" + dataInizio + "\",\"dataFine\":\"" + dataFine + "\"}";
    }

    private String payloadConLimite(String dataInizio, String dataFine, String dataLimite) {
        return "{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"Tour della Sila\",\"destinazionePrincipale\":\"Camigliatello\","
                + "\"prezzoBase\":149.90,\"maxPartecipanti\":15,"
                + "\"dataInizio\":\"" + dataInizio + "\",\"dataFine\":\"" + dataFine + "\","
                + "\"dataLimitePrenotazione\":\"" + dataLimite + "\"}";
    }

    private long creaItinerarioConPeriodo(LocalDate inizio, LocalDate fine, LocalDate limite) throws Exception {
        String corpo = limite == null
                ? payload(inizio.toString(), fine.toString())
                : payloadConLimite(inizio.toString(), fine.toString(), limite.toString());

        MvcResult creazione = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(creazione.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void laDurataVieneRicavataDalPeriodoEDiventaUnaDisponibilita() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(10);
        LocalDate fine = inizio.plusDays(4);

        MvcResult creazione = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(inizio.toString(), fine.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durataGiorni").value(5))
                .andExpect(jsonPath("$.dataInizio").value(inizio.toString()))
                .andExpect(jsonPath("$.dataFine").value(fine.toString()))
                .andReturn();

        long idItinerario = objectMapper.readTree(creazione.getResponse().getContentAsString())
                .get("id").asLong();

        List<DisponibilitaItinerario> disponibilita =
                disponibilitaRepository.findByItinerario_Id(idItinerario);

        assertThat(disponibilita)
                .as("il periodo indicato in creazione deve diventare una disponibilita' prenotabile")
                .singleElement()
                .satisfies(periodo -> {
                    assertThat(periodo.getDataInizio().toLocalDate()).isEqualTo(inizio);
                    assertThat(periodo.getDataFine().toLocalDate()).isEqualTo(fine);
                    assertThat(periodo.getPostiDisponibili()).isEqualTo(15);
                });
    }

    @Test
    void laDurataInviataDalClientNonPuoContraddireLeDate() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(3);

        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"maxPartecipanti\":2,\"durataGiorni\":99,"
                                + "\"dataInizio\":\"" + inizio + "\","
                                + "\"dataFine\":\"" + inizio.plusDays(1) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durataGiorni").value(2));
    }

    @ParameterizedTest(name = "periodo non valido: {0}")
    @CsvSource(delimiter = '|', value = {
            "inizio nel passato      | -1 | 3",
            "fine prima dell'inizio  | 5  | 4"
    })
    void iPeriodiNonValidiVengonoRifiutatiCon400(String caso, int giorniInizio, int giorniFine) throws Exception {
        String inizio = LocalDate.now().plusDays(giorniInizio).toString();
        String fine = LocalDate.now().plusDays(giorniFine).toString();

        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(inizio, fine)))
                .andExpect(status().isBadRequest());

        assertThat(itinerarioRepository.findAll())
                .as("un periodo non valido non deve creare nulla")
                .isEmpty();
    }

    @Test
    void unaSolaDelleDueDateVieneRifiutata() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"maxPartecipanti\":2,\"dataInizio\":\"" + LocalDate.now().plusDays(2) + "\"}"))
                .andExpect(status().isBadRequest());

        assertThat(itinerarioRepository.findAll()).isEmpty();
    }

    @Test
    void senzaDateESenzaDurataLaRichiestaVieneRifiutata() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"maxPartecipanti\":2}"))
                .andExpect(status().isBadRequest());

        assertThat(itinerarioRepository.findAll()).isEmpty();
    }

    @Test
    void laSolaDurataRestaAccettataSenzaCreareDisponibilita() throws Exception {
        MvcResult creazione = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"maxPartecipanti\":2,\"durataGiorni\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durataGiorni").value(3))
                .andExpect(jsonPath("$.dataInizio").doesNotExist())
                .andReturn();

        long idItinerario = objectMapper.readTree(creazione.getResponse().getContentAsString())
                .get("id").asLong();

        assertThat(disponibilitaRepository.findByItinerario_Id(idItinerario)).isEmpty();
    }

    @Test
    void laModificaAggiungeUnaPartenzaSenzaToccareQuellaGiaPubblicata() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(10);
        long idItinerario = creaItinerarioConPeriodo(inizio, inizio.plusDays(4), null);

        DisponibilitaItinerario periodo = disponibilitaRepository.findByItinerario_Id(idItinerario).get(0);
        periodo.setPostiDisponibili(6);
        disponibilitaRepository.save(periodo);

        LocalDate nuovoInizio = inizio.plusDays(20);
        LocalDate nuovaFine = nuovoInizio.plusDays(2);

        mockMvc.perform(put("/api/itinerari/" + idItinerario)
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(nuovoInizio.toString(), nuovaFine.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataInizio").value(inizio.toString()));

        assertThat(disponibilitaRepository.findByItinerario_Id(idItinerario))
                .as("la partenza gia' pubblicata resta, quella nuova si aggiunge")
                .hasSize(2);

        DisponibilitaItinerario originale = disponibilitaRepository.findByItinerario_Id(idItinerario).stream()
                .filter(d -> d.getDataInizio().toLocalDate().equals(inizio))
                .findFirst()
                .orElseThrow();

        assertThat(originale.getDataFine().toLocalDate()).isEqualTo(inizio.plusDays(4));
        assertThat(originale.getPostiDisponibili())
                .as("i posti della partenza gia' venduta non vengono ricalcolati")
                .isEqualTo(6);

        assertThat(disponibilitaRepository.findByItinerario_Id(idItinerario))
                .anySatisfy(nuova -> {
                    assertThat(nuova.getDataInizio().toLocalDate()).isEqualTo(nuovoInizio);
                    assertThat(nuova.getDataFine().toLocalDate()).isEqualTo(nuovaFine);
                });
    }

    @Test
    void unPeriodoUgualeAUnoGiaPresenteNonCreaUnDoppione() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(10);
        long idItinerario = creaItinerarioConPeriodo(inizio, inizio.plusDays(4), null);

        mockMvc.perform(put("/api/itinerari/" + idItinerario)
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(inizio.toString(), inizio.plusDays(4).toString())))
                .andExpect(status().isOk());

        assertThat(disponibilitaRepository.findByItinerario_Id(idItinerario)).hasSize(1);
    }

    @Test
    void aUnItinerarioSenzaDisponibilitaLaModificaNeAggiungeUna() throws Exception {
        MvcResult creazione = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"maxPartecipanti\":2,\"durataGiorni\":3}"))
                .andExpect(status().isOk())
                .andReturn();

        long idItinerario = objectMapper.readTree(creazione.getResponse().getContentAsString())
                .get("id").asLong();

        LocalDate inizio = LocalDate.now().plusDays(15);
        mockMvc.perform(put("/api/itinerari/" + idItinerario)
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(inizio.toString(), inizio.plusDays(1).toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durataGiorni").value(2));

        assertThat(disponibilitaRepository.findByItinerario_Id(idItinerario))
                .singleElement()
                .satisfies(periodo ->
                        assertThat(periodo.getDataInizio().toLocalDate()).isEqualTo(inizio));
    }

    @Test
    void ilTermineDiPrenotazioneVieneSalvatoEdEsposto() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(30);
        LocalDate limite = inizio.minusDays(7);

        long idItinerario = creaItinerarioConPeriodo(inizio, inizio.plusDays(3), limite);

        assertThat(disponibilitaRepository.findByItinerario_Id(idItinerario))
                .singleElement()
                .satisfies(periodo -> {
                    assertThat(periodo.getDataLimitePrenotazione().toLocalDate()).isEqualTo(limite);
                    assertThat(periodo.getDataLimitePrenotazione().getHour())
                            .as("il termine e' inclusivo: vale fino a fine giornata")
                            .isEqualTo(23);
                });

        mockMvc.perform(get("/api/itinerari/" + idItinerario)
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataLimitePrenotazione").value(limite.toString()));
    }

    @Test
    void ilTermineNonPuoSuperareLaPartenzaNeStareNelPassato() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(10);

        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConLimite(inizio.toString(), inizio.plusDays(3).toString(),
                                inizio.plusDays(1).toString())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConLimite(inizio.toString(), inizio.plusDays(3).toString(),
                                LocalDate.now().minusDays(1).toString())))
                .andExpect(status().isBadRequest());

        assertThat(itinerarioRepository.findAll()).isEmpty();
    }

    @Test
    void ilTermineSenzaLeDateDelViaggioVieneRifiutato() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"maxPartecipanti\":2,\"durataGiorni\":3,"
                                + "\"dataLimitePrenotazione\":\"" + LocalDate.now().plusDays(2) + "\"}"))
                .andExpect(status().isBadRequest());

        assertThat(itinerarioRepository.findAll()).isEmpty();
    }

    @Test
    void ilTermineDiUnaPartenzaGiaPubblicataNonCambia() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(30);
        LocalDate limite = inizio.minusDays(5);
        long idItinerario = creaItinerarioConPeriodo(inizio, inizio.plusDays(3), limite);

        mockMvc.perform(put("/api/itinerari/" + idItinerario)
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(inizio.toString(), inizio.plusDays(3).toString())))
                .andExpect(status().isOk());

        assertThat(disponibilitaRepository.findByItinerario_Id(idItinerario))
                .singleElement()
                .satisfies(periodo ->
                        assertThat(periodo.getDataLimitePrenotazione().toLocalDate())
                                .as("il termine gia' comunicato ai viaggiatori resta quello")
                                .isEqualTo(limite));
    }

    @Test
    void oltreIlTermineLaPrenotazioneVieneRifiutata() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(30);
        long idItinerario = creaItinerarioConPeriodo(inizio, inizio.plusDays(3), inizio.minusDays(5));

        DisponibilitaItinerario periodo = disponibilitaRepository.findByItinerario_Id(idItinerario).get(0);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + periodo.getId()
                                + ",\"numeroPartecipanti\":2}"))
                .andExpect(status().isCreated());

        DisponibilitaItinerario dopoPrenotazione =
                disponibilitaRepository.findById(periodo.getId()).orElseThrow();
        dopoPrenotazione.setDataLimitePrenotazione(LocalDateTime.now().minusMinutes(1));
        disponibilitaRepository.save(dopoPrenotazione);

        mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + periodo.getId()
                                + ",\"numeroPartecipanti\":2}"))
                .andExpect(status().isBadRequest());

        assertThat(disponibilitaRepository.findById(periodo.getId()).orElseThrow().getPostiDisponibili())
                .as("una prenotazione rifiutata non deve scalare posti")
                .isEqualTo(13);
    }

    @Test
    void senzaTermineSiPrenotaFinoAllaPartenza() throws Exception {
        LocalDate inizio = LocalDate.now().plusDays(30);
        long idItinerario = creaItinerarioConPeriodo(inizio, inizio.plusDays(3), null);

        DisponibilitaItinerario periodo = disponibilitaRepository.findByItinerario_Id(idItinerario).get(0);
        periodo.setDataInizio(LocalDateTime.now().minusDays(1));
        disponibilitaRepository.save(periodo);

        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + periodo.getId()
                                + ",\"numeroPartecipanti\":1}"))
                .andExpect(status().isBadRequest());
    }

}
