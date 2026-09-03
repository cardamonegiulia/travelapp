package com.unical.travelapp.backend.security.fase2_io;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidazioneInputSecurityTest extends SecurityIntegrationTestBase {

    private Utente organizzatore;
    private Utente viaggiatore;
    private Itinerario itinerarioEsistente;

    @BeforeEach
    void utentiDiProva() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        viaggiatore = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
        itinerarioEsistente = itinerario(organizzatore);
    }

    @ParameterizedTest(name = "itinerario non valido: {0}")
    @CsvSource(delimiter = '|', value = {
            "campo obbligatorio mancante  | {\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}",
            "titolo vuoto                 | {\"titolo\":\"\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}",
            "titolo di soli spazi         | {\"titolo\":\"   \",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}",
            "prezzo zero                  | {\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":0,\"durataGiorni\":1,\"maxPartecipanti\":2}",
            "prezzo negativo              | {\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":-5,\"durataGiorni\":1,\"maxPartecipanti\":2}",
            "durata negativa              | {\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,\"durataGiorni\":-1,\"maxPartecipanti\":2}",
            "partecipanti zero            | {\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":0}",
            "destinazione mancante        | {\"titolo\":\"T\",\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}"
    })
    void iPayloadDiItinerarioNonValidiVengonoRifiutatiCon400(String caso, String payload) throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(itinerarioRepository.findAll())
                .as("nessun itinerario deve essere creato da un payload non valido")
                .hasSize(1);
    }

    @Test
    void ilTitoloOltreILimiteDiSizeVieneRifiutato() throws Exception {
        String titoloLungo = "x".repeat(200);

        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"" + titoloLungo + "\",\"destinazionePrincipale\":\"D\","
                                + "\"prezzoBase\":10.0,\"durataGiorni\":1,\"maxPartecipanti\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.titolo").exists());
    }

    @Test
    void ilProblemDetailElencaICampiInErroreSenzaEsporreClassiOPackage() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"\",\"prezzoBase\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:travelapp:problem:validazione-fallita"))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.errori.titolo").exists())
                .andExpect(jsonPath("$.errori.destinazionePrincipale").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @ParameterizedTest(name = "prenotazione non valida: {0}")
    @CsvSource(delimiter = '|', value = {
            "partecipanti mancanti | {\"disponibilitaItinerarioId\":1}",
            "partecipanti zero     | {\"disponibilitaItinerarioId\":1,\"numeroPartecipanti\":0}",
            "partecipanti negativi | {\"disponibilitaItinerarioId\":1,\"numeroPartecipanti\":-3}",
            "id non positivo       | {\"disponibilitaItinerarioId\":-1,\"numeroPartecipanti\":1}",
            "id extra non positivo | {\"disponibilitaItinerarioId\":1,\"numeroPartecipanti\":1,\"attivitaExtraIds\":[-7]}"
    })
    void iPayloadDiPrenotazioneNonValidiVengonoRifiutatiCon400(String caso, String payload) throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(prenotazioneRepository.findAll()).isEmpty();
    }

    @ParameterizedTest(name = "recensione non valida: {0}")
    @CsvSource(delimiter = '|', value = {
            "voto sotto il minimo | {\"itinerarioId\":1,\"votazione\":0}",
            "voto sopra il massimo| {\"itinerarioId\":1,\"votazione\":6}",
            "voto negativo        | {\"itinerarioId\":1,\"votazione\":-1}",
            "id non positivo      | {\"itinerarioId\":-1,\"votazione\":3}"
    })
    void iPayloadDiRecensioneNonValidiVengonoRifiutatiCon400(String caso, String payload) throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(recensioneRepository.findAll()).isEmpty();
    }

    @Test
    void ilCommentoOltreIlLimiteVieneRifiutato() throws Exception {
        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":" + itinerarioEsistente.getId()
                                + ",\"votazione\":3,\"comm\":\"" + "a".repeat(2500) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unEnumNonValidoVieneRifiutatoCon400() throws Exception {
        MvcResult risultato = mockMvc.perform(put("/api/utenti/" + viaggiatore.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ada\",\"tema\":\"FLUORESCENTE\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @Test
    void unEmailMalformataVieneRifiutata() throws Exception {
        mockMvc.perform(put("/api/utenti/" + viaggiatore.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"non-una-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.email").exists());
    }

    @Test
    void unJsonMalformatoVieneRifiutatoCon400ENonCon500() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\": 1, \"votazione\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:travelapp:problem:payload-non-valido"))
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @Test
    void unTipoSbagliatoNelJsonVieneRifiutatoCon400() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itinerarioId\":\"non-un-numero\",\"votazione\":3}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @Test
    void unBodyVuotoVieneRifiutatoCon400() throws Exception {
        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unArrayAlPostoDiUnOggettoVieneRifiutatoCon400() throws Exception {
        mockMvc.perform(post("/api/recensioni")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itinerarioId\":1,\"votazione\":3}]"))
                .andExpect(status().isBadRequest());
    }
}
