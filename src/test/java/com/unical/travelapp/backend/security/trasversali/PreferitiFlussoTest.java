package com.unical.travelapp.backend.security.trasversali;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.models.ListaPreferiti;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PreferitiFlussoTest extends SecurityIntegrationTestBase {

    @Autowired private TransactionTemplate transazione;

    private Itinerario primo;
    private Itinerario secondo;

    private List<Long> itinerariPreferitiA(String subject) {
        return transazione.execute(stato -> {
            Utente utente = utenteRepository.findByKeycloakId(subject).orElseThrow();
            return listaPreferitiRepository.findByUtenteOrderByIdDesc(utente).stream()
                    .flatMap(lista -> lista.getItinerari().stream())
                    .map(Itinerario::getId)
                    .toList();
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
        return mockMvc.perform(post("/api/preferiti/itinerari")
                .with(TestJwt.conRuoliRealm(subject, "VIAGGIATORE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itinerarioId\":" + itinerarioId + "}")).andReturn();
    }

    private long creaLista(String subject, String nome, String visibilita) throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/preferiti")
                        .with(TestJwt.conRuoliRealm(subject, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"visibilita\":\"" + visibilita + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(risultato.getResponse().getContentAsString()).get("id").asLong();
    }

    private void condividi(String subject, long listaId, long utenteId) throws Exception {
        mockMvc.perform(post("/api/preferiti/" + listaId + "/condivisioni")
                        .with(TestJwt.conRuoliRealm(subject, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"utenteId\":" + utenteId + "}"))
                .andExpect(status().isOk());
    }

    private long idUtente(String subject) {
        return utenteRepository.findByKeycloakId(subject).orElseThrow().getId();
    }

    @Test
    void ilPrimoPreferitoDiUnUtenteVieneCreatoSenzaErroreInterno() throws Exception {
        MvcResult risultato = aggiungi(SUB_UTENTE_A, primo.getId());

        assertThat(risultato.getResponse().getStatus())
                .as("il primo preferito non deve provocare un errore interno")
                .isEqualTo(201);
        NessunLeak.verifica(risultato);

        assertThat(listaPreferitiRepository.count()).isEqualTo(1);
        assertThat(itinerariPreferitiA(SUB_UTENTE_A)).containsExactly(primo.getId());
    }

    @Test
    void ilSecondoPreferitoSiAggiungeAllaListaPredefinitaEsistente() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());
        MvcResult risultato = aggiungi(SUB_UTENTE_A, secondo.getId());

        assertThat(risultato.getResponse().getStatus()).isEqualTo(201);
        assertThat(listaPreferitiRepository.count())
                .as("il salvataggio rapido riusa sempre la stessa lista predefinita")
                .isEqualTo(1);
        assertThat(itinerariPreferitiA(SUB_UTENTE_A))
                .containsExactlyInAnyOrder(primo.getId(), secondo.getId());
    }

    @Test
    void loStessoItinerarioNonFinisceDueVolteNellaStessaLista() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());
        aggiungi(SUB_UTENTE_A, primo.getId());

        assertThat(itinerariPreferitiA(SUB_UTENTE_A)).containsExactly(primo.getId());
    }

    @Test
    void laListaPredefinitaNascePrivata() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());

        mockMvc.perform(get("/api/preferiti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value(ListaPreferiti.NOME_LISTA_PREDEFINITA))
                .andExpect(jsonPath("$[0].visibilita").value("PRIVATA"))
                .andExpect(jsonPath("$[0].proprietaria").value(true))
                .andExpect(jsonPath("$[0].numeroItinerari").value(1));
    }

    @Test
    void ilDettaglioDellaListaRestituisceGliItinerariSalvati() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());
        long listaId = listaPreferitiRepository.findAll().get(0).getId();

        Utente utenteA = utenteRepository.findByKeycloakId(SUB_UTENTE_A).orElseThrow();
        mockMvc.perform(get("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proprietarioId").value(utenteA.getId().intValue()))
                .andExpect(jsonPath("$.itinerari.length()").value(1))
                .andExpect(jsonPath("$.itinerari[0].id").value(primo.getId().intValue()));
    }

    @Test
    void unPreferitoPuoEssereRimosso() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());
        aggiungi(SUB_UTENTE_A, secondo.getId());

        mockMvc.perform(delete("/api/preferiti/itinerari/" + primo.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isNoContent());

        assertThat(itinerariPreferitiA(SUB_UTENTE_A))
                .as("resta solo l'itinerario non rimosso")
                .containsExactly(secondo.getId());
    }

    @Test
    void leListeDiDueUtentiRestanoSeparate() throws Exception {
        aggiungi(SUB_UTENTE_A, primo.getId());
        aggiungi(SUB_UTENTE_B, secondo.getId());

        assertThat(listaPreferitiRepository.count()).isEqualTo(2);
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
        assertThat(listaPreferitiRepository.findAll())
                .as("una richiesta fallita non deve lasciare una lista vuota a database")
                .isEmpty();
    }

    @Test
    void unViaggiatorePuoAvereListeDiverse() throws Exception {
        creaLista(SUB_UTENTE_A, "Estate", "PRIVATA");
        creaLista(SUB_UTENTE_A, "Idee per il weekend", "CONDIVISA");

        mockMvc.perform(get("/api/preferiti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void dueListeDelloStessoUtenteNonPossonoAvereLoStessoNome() throws Exception {
        creaLista(SUB_UTENTE_A, "Estate", "PRIVATA");

        MvcResult risultato = mockMvc.perform(post("/api/preferiti")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"estate\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @Test
    void unaListaPrivataNonEVisibileAgliAltriUtenti() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Estate", "PRIVATA");

        MvcResult risultato = mockMvc.perform(get("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isNotFound())
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @Test
    void unaListaPrivataNonCompareFraQuelleCondiviseConMe() throws Exception {
        creaLista(SUB_UTENTE_A, "Estate", "PRIVATA");

        mockMvc.perform(get("/api/preferiti/condivise-con-me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ilDestinatarioLeggeLaListaCondivisaConLui() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Idee per il weekend", "CONDIVISA");
        mockMvc.perform(post("/api/preferiti/" + listaId + "/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":" + primo.getId() + "}"))
                .andExpect(status().isCreated());

        condividi(SUB_UTENTE_A, listaId, idUtente(SUB_UTENTE_B));

        mockMvc.perform(get("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proprietaria").value(false))
                .andExpect(jsonPath("$.itinerari.length()").value(1));

        mockMvc.perform(get("/api/preferiti/condivise-con-me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) listaId));
    }

    @Test
    void unUtenteNonDestinatarioNonVedeLaListaCondivisa() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Idee per il weekend", "CONDIVISA");

        MvcResult risultato = mockMvc.perform(get("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isNotFound())
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @Test
    void ilDestinatarioNonPuoModificareLaListaCheLegge() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Idee per il weekend", "CONDIVISA");
        condividi(SUB_UTENTE_A, listaId, idUtente(SUB_UTENTE_B));

        MvcResult aggiunta = mockMvc.perform(post("/api/preferiti/" + listaId + "/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":" + primo.getId() + "}"))
                .andExpect(status().isForbidden())
                .andReturn();
        NessunLeak.verifica(aggiunta);

        mockMvc.perform(delete("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isForbidden());

        assertThat(listaPreferitiRepository.existsById(listaId))
                .as("la lista di A non deve poter essere cancellata da B")
                .isTrue();
    }

    @Test
    void ilDestinatarioNonVedeConChiAltroLaListaECondivisa() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Idee per il weekend", "CONDIVISA");
        condividi(SUB_UTENTE_A, listaId, idUtente(SUB_UTENTE_B));
        condividi(SUB_UTENTE_A, listaId, idUtente(SUB_ORGANIZZATORE));

        mockMvc.perform(get("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinatari.length()").value(0));

        mockMvc.perform(get("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinatari.length()").value(2));
    }

    @Test
    void revocareLaCondivisioneToglieLAccesso() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Idee per il weekend", "CONDIVISA");
        long idB = idUtente(SUB_UTENTE_B);
        condividi(SUB_UTENTE_A, listaId, idB);

        mockMvc.perform(delete("/api/preferiti/" + listaId + "/condivisioni/" + idB)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void riportareLaListaAPrivataRevocaTutteLeCondivisioni() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Idee per il weekend", "CONDIVISA");
        condividi(SUB_UTENTE_A, listaId, idUtente(SUB_UTENTE_B));

        mockMvc.perform(put("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Idee per il weekend\",\"visibilita\":\"PRIVATA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibilita").value("PRIVATA"))
                .andExpect(jsonPath("$.destinatari.length()").value(0));

        mockMvc.perform(get("/api/preferiti/" + listaId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/preferiti/condivise-con-me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void condividereUnaListaPrivataLaRendeCondivisa() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Estate", "PRIVATA");

        mockMvc.perform(post("/api/preferiti/" + listaId + "/condivisioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + utenteRepository.findByKeycloakId(SUB_UTENTE_B).orElseThrow().getEmail() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibilita").value("CONDIVISA"))
                .andExpect(jsonPath("$.destinatari.length()").value(1));
    }

    @Test
    void nessunoPuoCondividereUnaListaAltrui() throws Exception {
        long listaId = creaLista(SUB_UTENTE_A, "Estate", "PRIVATA");

        MvcResult risultato = mockMvc.perform(post("/api/preferiti/" + listaId + "/condivisioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"utenteId\":" + idUtente(SUB_UTENTE_B) + "}"))
                .andExpect(status().isNotFound())
                .andReturn();

        NessunLeak.verifica(risultato);
    }
}
