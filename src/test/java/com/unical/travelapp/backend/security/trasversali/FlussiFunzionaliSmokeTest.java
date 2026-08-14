package com.unical.travelapp.backend.security.trasversali;

import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
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

/**
 * Test di regressione funzionale: l'hardening non deve aver rotto il percorso legittimo.
 *
 * <p>Un controllo di sicurezza che blocca anche gli usi corretti viene disattivato al primo
 * incidente in produzione, quindi va verificato che il flusso normale funzioni ancora.
 */
class FlussiFunzionaliSmokeTest extends SecurityIntegrationTestBase {

    private Utente organizzatore;

    @BeforeEach
    void utentiDiBase() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
    }

    @Test
    void unNuovoUtenteSiRegistraTramiteSincronizzazione() throws Exception {
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conEmail("sub-nuovo-viaggiatore", "nuovo@example.test", "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ruolo").value("VIAGGIATORE"));

        assertThat(utenteRepository.findByKeycloakId("sub-nuovo-viaggiatore")).isPresent();
    }

    @Test
    void laSincronizzazioneAssegnaIlRuoloDelTokenNonUnDefaultFisso() throws Exception {
        // un ORGANIZZATORE creato fuori dal flusso self-service deve risultare ORGANIZZATORE
        // anche in locale: altrimenti il frontend, che legge UtenteResponseDto.ruolo, mostra
        // un ruolo diverso da quello che il backend applica nei @PreAuthorize
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conEmail("sub-nuovo-organizzatore", "org@example.test", "ORGANIZZATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("ORGANIZZATORE"));

        assertThat(utenteRepository.findByKeycloakId("sub-nuovo-organizzatore").orElseThrow().getRuolo())
                .isEqualTo(Ruolo.ORGANIZZATORE);
    }

    @Test
    void laSincronizzazioneRifiutaUnTokenSenzaEmailInveceDiSalvarneUnaVuota() throws Exception {
        // l'email e' chiave unica: salvarla vuota bruciava il valore per tutti gli utenti
        // successivi, che fallivano sul vincolo di unicita' con un errore incomprensibile
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm("sub-senza-email", "VIAGGIATORE")))
                .andExpect(status().isBadRequest());

        assertThat(utenteRepository.findByKeycloakId("sub-senza-email")).isEmpty();
    }

    @Test
    void laSincronizzazioneNonRubaLEmailDiUnUtenteGiaEsistente() throws Exception {
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conEmail("sub-altro-subject", organizzatore.getEmail(), "VIAGGIATORE")))
                .andExpect(status().isConflict());

        assertThat(utenteRepository.findByKeycloakId("sub-altro-subject")).isEmpty();
    }

    @Test
    void unOrganizzatoreCreaUnItinerarioEUnViaggiatoreLoVede() throws Exception {
        MvcResult creazione = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Tour della Sila\",\"descrizione\":\"Tre giorni in montagna\","
                                + "\"destinazionePrincipale\":\"Camigliatello\",\"prezzoBase\":149.90,"
                                + "\"durataGiorni\":3,\"maxPartecipanti\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titolo").value("Tour della Sila"))
                .andExpect(jsonPath("$.stato").value("BOZZA"))
                .andReturn();

        long idItinerario = objectMapper.readTree(creazione.getResponse().getContentAsString())
                .get("id").asLong();

        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        mockMvc.perform(get("/api/itinerari/" + idItinerario)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titolo").value("Tour della Sila"));
    }

    @Test
    void ilFlussoCompletoPrenotazionePagamentoRecensioneFunziona() throws Exception {
        Utente viaggiatore = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        Itinerario itinerario = itinerario(organizzatore);
        DisponibilitaItinerario disponibilita = disponibilita(itinerario, 10);

        // 1. prenotazione
        MvcResult prenotazione = mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilitaItinerarioId\":" + disponibilita.getId()
                                + ",\"numeroPartecipanti\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.viaggiatoreId").value(viaggiatore.getId().intValue()))
                .andExpect(jsonPath("$.statoPrenotazione").value("IN_ATTESA"))
                .andReturn();

        long idPrenotazione = objectMapper.readTree(prenotazione.getResponse().getContentAsString())
                .get("id").asLong();

        // i posti sono stati scalati
        assertThat(disponibilitaRepository.findById(disponibilita.getId()).orElseThrow()
                .getPostiDisponibili()).isEqualTo(8);

        // 2. pagamento
        mockMvc.perform(post("/api/prenotazioni/" + idPrenotazione + "/paga")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statoPrenotazione").value("CONFERMATA"))
                .andExpect(jsonPath("$.statoPagamento").value("COMPLETATO"));

        assertThat(prenotazioneRepository.findById(idPrenotazione).orElseThrow().getStato())
                .isEqualTo(StatoPrenotazione.CONFERMATA);
        assertThat(pagamentoRepository.findByPrenotazioneId(idPrenotazione).orElseThrow().getStato())
                .isEqualTo(StatoPagamento.COMPLETATO);

        // 3. recensione della prenotazione pagata
        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenotazioneId\":" + idPrenotazione
                                + ",\"itinerarioId\":" + itinerario.getId()
                                + ",\"votazione\":5,\"comm\":\"Esperienza ottima\"}"))
                .andExpect(status().isCreated());

        // 4. la recensione compare nell'elenco e nella media
        mockMvc.perform(get("/api/recensioni/itinerario/" + itinerario.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/recensioni/itinerario/" + itinerario.getId() + "/media")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5.0));
    }

    @Test
    void lAnnullamentoRestituisceIPosti() throws Exception {
        Utente viaggiatore = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        Itinerario itinerario = itinerario(organizzatore);
        DisponibilitaItinerario disponibilita = disponibilita(itinerario, 10);
        var prenotazione = prenotazione(viaggiatore, disponibilita);

        mockMvc.perform(post("/api/prenotazioni/" + prenotazione.getId() + "/annulla")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statoPrenotazione").value("CANCELLATA"));

        assertThat(disponibilitaRepository.findById(disponibilita.getId()).orElseThrow()
                .getPostiDisponibili())
                .as("i posti della prenotazione annullata tornano disponibili")
                .isEqualTo(12);
    }

    @Test
    void unViaggiatoreConsultaLeProprieePrenotazioni() throws Exception {
        Utente viaggiatore = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        Itinerario itinerario = itinerario(organizzatore);
        DisponibilitaItinerario disponibilita = disponibilita(itinerario, 10);
        prenotazione(viaggiatore, disponibilita);
        prenotazione(viaggiatore, disponibilita);

        mockMvc.perform(get("/api/prenotazioni/utente/" + viaggiatore.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void unAdminAmministraGliUtenti() throws Exception {
        utente(SUB_ADMIN, Ruolo.ADMIN);

        MvcResult creazione = mockMvc.perform(post("/api/utenti")
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keycloakId\":\"sub-creato-da-admin\",\"nome\":\"Grace\","
                                + "\"cognome\":\"Hopper\",\"email\":\"grace@example.test\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long id = objectMapper.readTree(creazione.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/utenti/" + id)
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Grace"));

        mockMvc.perform(get("/api/utenti")
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void unOrganizzatoreGestisceIlProprioCatalogo() throws Exception {
        MvcResult creazione = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Da cancellare\",\"destinazionePrincipale\":\"D\","
                                + "\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}"))
                .andExpect(status().isOk())
                .andReturn();

        long id = objectMapper.readTree(creazione.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/itinerari/" + id)
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isNoContent());

        assertThat(itinerarioRepository.existsById(id)).isFalse();
    }

    @Test
    void lElencoDelCatalogoEPaginatoEAccessibileAOgniUtenteAutenticato() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        for (int i = 0; i < 3; i++) {
            itinerario(organizzatore);
        }

        mockMvc.perform(get("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/attivita")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk());
    }
}
