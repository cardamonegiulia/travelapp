package com.unical.travelapp.backend.identity;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.IdentityProviderNonDisponibileException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CancellazioneUtenteTest extends SecurityIntegrationTestBase {

    private static final String TOKEN_ADMIN = "token-service-account";

    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void keycloakRisponde() {
        when(keycloakAdminClient.ottieniTokenAmministrativo()).thenReturn(TOKEN_ADMIN);
    }

    @Test
    void laCancellazioneRimuoveLUtenteAncheDaKeycloak() throws Exception {
        Utente bersaglio = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);

        mockMvc.perform(delete("/api/utenti/" + bersaglio.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isNoContent());

        verify(keycloakAdminClient).eliminaUtente(TOKEN_ADMIN, SUB_UTENTE_B);
        assertThat(utenteRepository.existsById(bersaglio.getId()))
                .as("il record locale deve sparire")
                .isFalse();
    }

    @Test
    void seKeycloakNonCancellaIlRecordLocaleResta() throws Exception {
        Utente bersaglio = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);

        doThrow(new IdentityProviderNonDisponibileException("Keycloak non raggiungibile"))
                .when(keycloakAdminClient).eliminaUtente(anyString(), anyString());

        MvcResult risultato = mockMvc.perform(delete("/api/utenti/" + bersaglio.getId())
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .as("il motivo tecnico resta nei log, non nel body")
                .doesNotContain("Keycloak");
        assertThat(utenteRepository.existsById(bersaglio.getId()))
                .as("meglio una cancellazione fallita che una divergenza silenziosa")
                .isTrue();
    }

    @Test
    void suUtenteInesistenteNonSiChiamaNemmenoKeycloak() throws Exception {
        utente(SUB_ADMIN, Ruolo.ADMIN);

        mockMvc.perform(delete("/api/utenti/999999")
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isNotFound());

        verify(keycloakAdminClient, never()).eliminaUtente(anyString(), anyString());
    }

    @Test
    void unUtenteCancellaSeStessoEQuindiAncheLaPropriaIdentita() throws Exception {
        Utente proprio = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(delete("/api/utenti/" + proprio.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isNoContent());

        verify(keycloakAdminClient).eliminaUtente(TOKEN_ADMIN, SUB_UTENTE_A);
    }

    @Test
    void unaCancellazioneNegataNonToccaKeycloak() throws Exception {
        Utente altrui = utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(delete("/api/utenti/" + altrui.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isForbidden());

        verify(keycloakAdminClient, never()).eliminaUtente(anyString(), anyString());
        assertThat(utenteRepository.existsById(altrui.getId())).isTrue();
    }
}
