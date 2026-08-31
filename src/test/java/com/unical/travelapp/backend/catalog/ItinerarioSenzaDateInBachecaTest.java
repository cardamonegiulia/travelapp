package com.unical.travelapp.backend.catalog;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un itinerario senza date prenotabili resta in bacheca.
 *
 * <p>L'assenza di partenze non e' uno stato definitivo: l'organizzatore puo' aggiungerne di
 * nuove quando vuole. L'itinerario sparisce dall'elenco in un solo caso, quello esplicito:
 * l'organizzatore lo elimina.
 */
@DisplayName("Itinerario senza date disponibili")
class ItinerarioSenzaDateInBachecaTest extends SecurityIntegrationTestBase {

    private Utente organizzatore;

    @BeforeEach
    void datiDiPartenza() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
    }

    @Test
    void unItinerarioSenzaNessunaPartenzaRestaVisibileConLIndicazione() throws Exception {
        Itinerario senzaDate = itinerario(organizzatore);

        mockMvc.perform(get("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(senzaDate.getId().intValue()))
                .andExpect(jsonPath("$.content[0].dateDisponibili").value(false));
    }

    @Test
    void unItinerarioConSolePartenzePassateRestaVisibile() throws Exception {
        Itinerario itinerario = itinerario(organizzatore);
        disponibilitaConclusa(itinerario, 10);

        mockMvc.perform(get("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].dateDisponibili").value(false));
    }

    @Test
    void unaNuovaPartenzaRiportaLItinerarioFraQuelliPrenotabili() throws Exception {
        Itinerario itinerario = itinerario(organizzatore);
        disponibilitaConclusa(itinerario, 10);

        // l'organizzatore programma una nuova partenza
        DisponibilitaItinerario nuova = new DisponibilitaItinerario();
        nuova.setItinerario(itinerario);
        nuova.setDataInizio(LocalDateTime.now().plusDays(20));
        nuova.setDataFine(LocalDateTime.now().plusDays(24));
        nuova.setPostiDisponibili(10);
        disponibilitaRepository.save(nuova);

        mockMvc.perform(get("/api/itinerari/" + itinerario.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateDisponibili").value(true));
    }

    @Test
    void unItinerarioEsauritoNonEUnItinerarioSenzaDate() throws Exception {
        Itinerario itinerario = itinerario(organizzatore);
        disponibilita(itinerario, 0); // partenza futura, zero posti

        // "esaurito" e "nessuna data" sono due cose diverse: qui le date ci sono
        mockMvc.perform(get("/api/itinerari/" + itinerario.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateDisponibili").value(true));
    }

    @Test
    void lItinerarioSparisceSoloQuandoLOrganizzatoreLoElimina() throws Exception {
        Itinerario senzaDate = itinerario(organizzatore);

        mockMvc.perform(delete("/api/itinerari/" + senzaDate.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isNoContent());

        assertThat(itinerarioRepository.existsById(senzaDate.getId())).isFalse();

        mockMvc.perform(get("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
