package com.unical.travelapp.backend.catalog;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le liste che il viaggiatore legge prima di prenotare (partenze di un itinerario, sessioni di
 * un'attività) devono arrivare come DTO piatti.
 *
 * <p>Restituire l'entità JPA non è solo una questione di stile: la disponibilità rimanda
 * all'itinerario, che contiene a sua volta le disponibilità, quindi Jackson entrava in un ciclo
 * infinito e la schermata non riceveva né date né posti.
 */
class DisponibilitaEsposteTest extends SecurityIntegrationTestBase {

    private Utente organizzatore;

    @BeforeEach
    void datiDiBase() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
    }

    @Test
    void leDisponibilitaDiUnItinerarioArrivanoConDatePostiELimite() throws Exception {
        Itinerario itinerario = itinerario(organizzatore);
        DisponibilitaItinerario periodo = disponibilita(itinerario, 12);
        periodo.setDataLimitePrenotazione(LocalDateTime.now().plusDays(20));
        disponibilitaRepository.save(periodo);

        mockMvc.perform(get("/api/itinerari/" + itinerario.getId() + "/disponibilita")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(periodo.getId().intValue()))
                .andExpect(jsonPath("$[0].dataInizio").exists())
                .andExpect(jsonPath("$[0].dataFine").exists())
                .andExpect(jsonPath("$[0].dataLimitePrenotazione").exists())
                .andExpect(jsonPath("$[0].postiDisponibili").value(12))
                // niente entità annidata: era la causa del ciclo in serializzazione
                .andExpect(jsonPath("$[0].itinerario").doesNotExist());
    }

    @Test
    void lePartenzeConcluseNonCompaionoFraLeDisponibilita() throws Exception {
        Itinerario itinerario = itinerario(organizzatore);
        disponibilitaConclusa(itinerario, 10);
        DisponibilitaItinerario futura = disponibilita(itinerario, 10);

        // una data che non si puo' piu' prenotare non arriva affatto: la scheda del
        // viaggiatore proponeva altrimenti partenze di viaggi gia' conclusi
        mockMvc.perform(get("/api/itinerari/" + itinerario.getId() + "/disponibilita")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(futura.getId().intValue()));
    }

    @Test
    void unaPartenzaColTermineDiPrenotazioneScadutoNonCompare() throws Exception {
        Itinerario itinerario = itinerario(organizzatore);
        DisponibilitaItinerario partenza = disponibilita(itinerario, 12);
        // il viaggio deve ancora partire, ma le prenotazioni si sono chiuse ieri
        partenza.setDataLimitePrenotazione(LocalDateTime.now().minusDays(1));
        disponibilitaRepository.save(partenza);

        mockMvc.perform(get("/api/itinerari/" + itinerario.getId() + "/disponibilita")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unaPartenzaEsauritaRestaInElenco() throws Exception {
        Itinerario itinerario = itinerario(organizzatore);
        DisponibilitaItinerario esaurita = disponibilita(itinerario, 0);

        // "esaurito" non e' "concluso": i posti possono tornare liberi, la data resta
        mockMvc.perform(get("/api/itinerari/" + itinerario.getId() + "/disponibilita")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(esaurita.getId().intValue()))
                .andExpect(jsonPath("$[0].postiDisponibili").value(0));
    }

    @Test
    void leSessioniDiUnAttivitaArrivanoConDateEPosti() throws Exception {
        SessioneSingolaAttivita sessione = sessione(organizzatore, 8);

        mockMvc.perform(get("/api/attivita/" + sessione.getSingolaAttivita().getId() + "/sessioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessione.getId().intValue()))
                .andExpect(jsonPath("$[0].dataInizio").exists())
                .andExpect(jsonPath("$[0].dataFine").exists())
                .andExpect(jsonPath("$[0].postiDisponibili").value(8))
                .andExpect(jsonPath("$[0].singolaAttivita").doesNotExist());
    }
}
