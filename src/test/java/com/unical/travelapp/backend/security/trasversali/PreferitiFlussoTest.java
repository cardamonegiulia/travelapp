package com.unical.travelapp.backend.security.trasversali;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressione per il finding F-06: il primo preferito di un utente andava in
 * NullPointerException perche' {@code Preferito.itinerario} non era inizializzata.
 *
 * <p>Il caso critico e' proprio il PRIMO inserimento, quando la riga Preferito non esiste
 * ancora e il service ne costruisce una nuova con {@code new Preferito()}.
 */
class PreferitiFlussoTest extends SecurityIntegrationTestBase {

    @Autowired private TransactionTemplate transazione;

    private Itinerario primo;
    private Itinerario secondo;

    /**
     * Legge gli id degli itinerari preferiti di un utente dentro una transazione: la
     * @ManyToMany e' LAZY e fuori da una sessione Hibernate non sarebbe inizializzabile.
     */
    private List<Long> itinerariPreferitiA(String subject) {
        return transazione.execute(stato -> {
            Utente utente = utenteRepository.findByKeycloakId(subject).orElseThrow();
            var preferito = preferitoRepository.findByUtente(utente);
            return preferito == null
                    ? List.<Long>of()
                    : preferito.getItinerario().stream().map(Itinerario::getId).toList();
        });
    }

    @BeforeEach
    void catalogoEUtenti() {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        primo = itinerario(organizzatore);
        secondo = itinerario(organizzatore);
    }

    private MvcResult aggiungi(String subject, long itinerarioId) throws Exception {
        return mockMvc.perform(post("/api/preferiti")
                .with(TestJwt.conRuoliRealm(subject, "VIAGGIATORE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itinerarioId\":" + itinerarioId + "}")).andReturn();
    }

    @Test
    void ilPrimoPreferitoDiUnUtenteVieneCreatoSenzaErroreInterno() throws Exception {
        MvcResult risultato = aggiungi(SUB_UTENTE_A, primo.getId());

        assertThat(risultato.getResponse().getStatus())
                .as("il primo preferito non deve provocare un errore interno")
                .isEqualTo(201);
        NessunLeak.verifica(risultato);

        assertThat(preferitoRepository.count()).isEqualTo(1);
        assertThat(itinerariPreferitiA(SUB_UTENTE_A)).containsExactly(primo.getId());
    }

    @Test
    void ilSecondoPreferitoSiAggiungeAllaListaEsistente() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());
        MvcResult risultato = aggiungi(SUB_UTENTE_A, secondo.getId());

        assertThat(risultato.getResponse().getStatus()).isEqualTo(201);
        assertThat(preferitoRepository.count())
                .as("una sola lista per utente")
                .isEqualTo(1);
        assertThat(itinerariPreferitiA(SUB_UTENTE_A))
                .containsExactlyInAnyOrder(primo.getId(), secondo.getId());
    }

    @Test
    void laListaDeiPreferitiVieneRestituitaDopoIlPrimoInserimento() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());

        Utente utenteA = utenteRepository.findByKeycloakId(SUB_UTENTE_A).orElseThrow();
        mockMvc.perform(get("/api/preferiti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utenteId").value(utenteA.getId().intValue()))
                .andExpect(jsonPath("$.itinerariList.length()").value(1))
                .andExpect(jsonPath("$.itinerariList[0].id").value(primo.getId().intValue()));
    }

    @Test
    void unPreferitoPuoEssereRimosso() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());
        aggiungi(SUB_UTENTE_A, secondo.getId());

        mockMvc.perform(delete("/api/preferiti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":" + primo.getId() + "}"))
                .andExpect(status().isOk());

        assertThat(itinerariPreferitiA(SUB_UTENTE_A))
                .as("resta solo l'itinerario non rimosso")
                .containsExactly(secondo.getId());
    }

    @Test
    void leListeDiDueUtentiRestanoSeparate() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());
        aggiungi(SUB_UTENTE_B, secondo.getId());

        assertThat(preferitoRepository.count()).isEqualTo(2);
        assertThat(itinerariPreferitiA(SUB_UTENTE_A)).containsExactly(primo.getId());
        assertThat(itinerariPreferitiA(SUB_UTENTE_B))
                .as("il preferito di A non deve finire nella lista di B")
                .containsExactly(secondo.getId());
    }

    @Test
    void unItinerarioInesistenteNonCreaUnaListaFantasma() throws Exception {
        MvcResult risultato = aggiungi(SUB_UTENTE_A, 999999L);

        assertThat(risultato.getResponse().getStatus()).isEqualTo(404);
        NessunLeak.verifica(risultato);
        assertThat(preferitoRepository.findAll())
                .as("una richiesta fallita non deve lasciare una lista vuota a database")
                .isEmpty();
    }
}
