package com.unical.travelapp.backend.security.fase3_risorse;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 3 - paginazione obbligatoria con tetto lato server.
 *
 * <p>Senza il tetto, {@code ?size=1000000} sarebbe una richiesta di estrazione massiva del
 * database con un solo GET: costo di memoria e di banda a carico del server e possibile
 * esfiltrazione in blocco.
 */
class PaginazioneSecurityTest extends SecurityIntegrationTestBase {

    @BeforeEach
    void catalogoAmpio() {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
        for (int i = 0; i < 25; i++) {
            itinerario(organizzatore);
        }
    }

    @ParameterizedTest(name = "size richiesto = {0} -> non oltre 100")
    @ValueSource(strings = {"101", "500", "10000", "2147483647"})
    void laDimensioneDiPaginaEClampataAlMassimoConfigurato(String size) throws Exception {
        mockMvc.perform(get("/api/itinerari").param("size", size)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize").value(100));
    }

    @Test
    void senzaParametriSiApplicaLaDimensioneDiPaginaDiDefault() throws Exception {
        mockMvc.perform(get("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.content.length()").value(20));
    }

    @ParameterizedTest(name = "parametri negativi: size={0}")
    @ValueSource(strings = {"-1", "0", "-2147483648"})
    void iParametriNegativiNonProvocanoErroriInterni(String size) throws Exception {
        MvcResult risultato = mockMvc.perform(get("/api/itinerari")
                        .param("size", size)
                        .param("page", "-1")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andReturn();

        assertThat(risultato.getResponse().getStatus())
                .as("parametri di paginazione assurdi non devono rompere il server")
                .isNotEqualTo(500);
        NessunLeak.verifica(risultato);
    }

    @Test
    void ilNumeroDiPaginaNegativoVieneRiportatoAllaPrimaPagina() throws Exception {
        mockMvc.perform(get("/api/itinerari").param("page", "-5")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0));
    }

    @Test
    void laStrutturaPageEPresenteNelBody() throws Exception {
        mockMvc.perform(get("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.number").exists())
                .andExpect(jsonPath("$.size").exists());
    }

    @Test
    void unNumeroDiPaginaOltreILimitiRestituisceUnaPaginaVuotaNonUnErrore() throws Exception {
        mockMvc.perform(get("/api/itinerari").param("page", "9999")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void ilTettoValeAnchePerGliEndpointRiservatiAgliAdmin() throws Exception {
        mockMvc.perform(get("/api/utenti").param("size", "5000")
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize").value(100));
    }

    @Test
    void ilTettoValeAncheSullElencoDelleRecensioni() throws Exception {
        mockMvc.perform(get("/api/recensioni/itinerario/1").param("size", "5000")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize").value(100));
    }

    @Test
    void ilTettoValeAncheSullElencoDellePrenotazioni() throws Exception {
        Utente utenteA = utenteRepository.findByKeycloakId(SUB_UTENTE_A).orElseThrow();

        mockMvc.perform(get("/api/prenotazioni/utente/" + utenteA.getId()).param("size", "5000")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize").value(100));
    }

    @Test
    void unOrdinamentoLegittimoContinuaAFunzionare() throws Exception {
        mockMvc.perform(get("/api/itinerari").param("sort", "id,desc")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
