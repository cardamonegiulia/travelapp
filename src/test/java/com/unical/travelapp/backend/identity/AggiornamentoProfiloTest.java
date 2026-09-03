package com.unical.travelapp.backend.identity;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Tema;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.IdentityProviderNonDisponibileException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.ProfiloKeycloak;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AggiornamentoProfiloTest extends SecurityIntegrationTestBase {

    private static final String TOKEN_ADMIN = "token-service-account";

    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void keycloakRisponde() {
        when(keycloakAdminClient.ottieniTokenAmministrativo()).thenReturn(TOKEN_ADMIN);
    }

    @Test
    void ilCambioEmailArrivaAKeycloak() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(put("/api/utenti/" + utente.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuova@example.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuova@example.test"));

        ArgumentCaptor<ProfiloKeycloak> profilo = ArgumentCaptor.forClass(ProfiloKeycloak.class);
        verify(keycloakAdminClient).aggiornaProfilo(eq(TOKEN_ADMIN), eq(SUB_UTENTE_A), profilo.capture(), eq(true));
        assertThat(profilo.getValue().email()).isEqualTo("nuova@example.test");

        assertThat(utenteRepository.findById(utente.getId()).orElseThrow().getEmail())
                .isEqualTo("nuova@example.test");
    }

    @Test
    void laVerificaEmailVieneChiestaSoloQuandoLIndirizzoCambia() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(put("/api/utenti/" + utente.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ada\"}"))
                .andExpect(status().isOk());

        verify(keycloakAdminClient).aggiornaProfilo(anyString(), anyString(), any(), eq(false));
    }

    @Test
    void ancheNomeECognomeVengonoPropagati() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(put("/api/utenti/" + utente.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ada\",\"cognome\":\"Lovelace\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProfiloKeycloak> profilo = ArgumentCaptor.forClass(ProfiloKeycloak.class);
        verify(keycloakAdminClient).aggiornaProfilo(eq(TOKEN_ADMIN), eq(SUB_UTENTE_A), profilo.capture(), eq(false));
        assertThat(profilo.getValue().nome()).isEqualTo("Ada");
        assertThat(profilo.getValue().cognome()).isEqualTo("Lovelace");
        assertThat(profilo.getValue().email())
                .as("i campi non modificati viaggiano col valore corrente, non come null")
                .isEqualTo(utente.getEmail());
    }

    @Test
    void cambiareSoloIlTemaNonDisturbaKeycloak() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(put("/api/utenti/" + utente.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tema\":\"SCURO\"}"))
                .andExpect(status().isOk());

        verifyNoInteractions(keycloakAdminClient);
        assertThat(utenteRepository.findById(utente.getId()).orElseThrow().getTema())
                .isEqualTo(Tema.SCURO);
    }

    @Test
    void unaRichiestaCheNonCambiaNullaNonChiamaKeycloak() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(put("/api/utenti/" + utente.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + utente.getNome() + "\"}"))
                .andExpect(status().isOk());

        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void lEmailVieneNormalizzataPrimaDiArrivareAKeycloak() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(put("/api/utenti/" + utente.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Nuova.Email@Example.TEST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuova.email@example.test"));

        ArgumentCaptor<ProfiloKeycloak> profilo = ArgumentCaptor.forClass(ProfiloKeycloak.class);
        verify(keycloakAdminClient).aggiornaProfilo(anyString(), anyString(), profilo.capture(), eq(true));
        assertThat(profilo.getValue().email()).isEqualTo("nuova.email@example.test");
    }

    @Test
    void seKeycloakRifiutaIlRecordLocaleNonCambia() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        String emailIniziale = utente.getEmail();

        doThrow(new IdentityProviderNonDisponibileException("Keycloak non raggiungibile"))
                .when(keycloakAdminClient).aggiornaProfilo(anyString(), anyString(), any(), anyBoolean());

        MvcResult risultato = mockMvc.perform(put("/api/utenti/" + utente.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuova@example.test\"}"))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(risultato.getResponse().getContentAsString()).doesNotContain("Keycloak");
        assertThat(utenteRepository.findById(utente.getId()).orElseThrow().getEmail())
                .as("nessuna scrittura locale se l'IdP non ha accettato la modifica")
                .isEqualTo(emailIniziale);
    }

    @Test
    void unEmailGiaUsataDaUnAltroUtenteVieneRifiutataPrimaDiChiamareKeycloak() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        Utente altro = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);

        mockMvc.perform(put("/api/utenti/" + utente.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + altro.getEmail() + "\"}"))
                .andExpect(status().isConflict());

        verify(keycloakAdminClient, never()).aggiornaProfilo(anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void unAggiornamentoNegatoNonToccaKeycloak() throws Exception {
        Utente altrui = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(put("/api/utenti/" + altrui.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rubata@example.test\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(keycloakAdminClient);
    }
}
