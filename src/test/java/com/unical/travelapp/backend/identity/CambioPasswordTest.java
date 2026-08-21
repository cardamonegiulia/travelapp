package com.unical.travelapp.backend.identity;

import com.unical.travelapp.backend.config.KeycloakRoleConverter;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.IdentityProviderNonDisponibileException;
import com.unical.travelapp.backend.identity.exception.PasswordNonConformeException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/utenti/me/password}.
 *
 * <p>Il cuore di questi test e' la condizione di freschezza: senza, il solo possesso di un
 * access token — che puo' essere stato rubato — basterebbe a cambiare la password e a
 * escludere il proprietario dal proprio account.
 */
class CambioPasswordTest extends SecurityIntegrationTestBase {

    private static final String TOKEN_ADMIN = "token-service-account";
    private static final String PASSWORD_VALIDA = "NuovaPassword1";

    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void keycloakRisponde() {
        when(keycloakAdminClient.ottieniTokenAmministrativo()).thenReturn(TOKEN_ADMIN);
    }

    /**
     * Token con {@code auth_time} esplicito. Non usa {@link TestJwt} perche' quel supporto
     * costruisce token per i test di autorizzazione, dove il momento del login e' irrilevante.
     */
    private JwtRequestPostProcessor tokenAutenticatoDa(String subject, Duration eta) {
        return jwt()
                .jwt(builder -> builder
                        .issuer(TestJwt.ISSUER)
                        .audience(List.of(TestJwt.CLIENT_ID))
                        .subject(subject)
                        .claim("preferred_username", subject)
                        .claim("auth_time", Instant.now().minus(eta))
                        .claim("realm_access", Map.of("roles", List.of("VIAGGIATORE"))))
                .authorities(new KeycloakRoleConverter(TestJwt.CLIENT_ID));
    }

    private String payload(String password) {
        return "{\"nuovaPassword\":\"" + password + "\"}";
    }

    // --- percorso felice ---------------------------------------------------

    @Test
    void conUnaAutenticazioneRecenteLaPasswordVieneCambiata() throws Exception {
        Utente utente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa(SUB_UTENTE_A, Duration.ofSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(PASSWORD_VALIDA)))
                .andExpect(status().isNoContent());

        verify(keycloakAdminClient).impostaPassword(TOKEN_ADMIN, SUB_UTENTE_A, PASSWORD_VALIDA);
        assertThat(utenteRepository.existsById(utente.getId())).isTrue();
    }

    @Test
    void dopoIlCambioLeSessioniVengonoChiuse() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa(SUB_UTENTE_A, Duration.ofSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(PASSWORD_VALIDA)))
                .andExpect(status().isNoContent());

        // l'ordine conta: chiudere le sessioni prima di aver cambiato la password lascerebbe
        // all'attaccante il tempo di rientrare con quella vecchia
        var ordine = inOrder(keycloakAdminClient);
        ordine.verify(keycloakAdminClient).impostaPassword(anyString(), anyString(), anyString());
        ordine.verify(keycloakAdminClient).terminaSessioniSenzaPropagareErrori(TOKEN_ADMIN, SUB_UTENTE_A);
    }

    // --- freschezza dell'autenticazione ------------------------------------

    @Test
    void unaAutenticazioneVecchiaRichiedeDiRifareIlLogin() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        MvcResult risultato = mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa(SUB_UTENTE_A, Duration.ofHours(3)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(PASSWORD_VALIDA)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        org.hamcrest.Matchers.containsString("insufficient_user_authentication")))
                .andExpect(jsonPath("$.maxAge").value(300))
                .andReturn();

        NessunLeak.verifica(risultato);
        verify(keycloakAdminClient, never()).impostaPassword(anyString(), anyString(), anyString());
    }

    @Test
    void unTokenSenzaAuthTimeNonBastaACambiareLaPassword() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        // TestJwt non mette auth_time: e' il caso di un token che non sa dire quando sia
        // avvenuto il login. Fail-closed, altrimenti il controllo sarebbe aggirabile
        // semplicemente omettendo il claim.
        mockMvc.perform(post("/api/utenti/me/password")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(PASSWORD_VALIDA)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void senzaTokenLaRottaRestaChiusa() throws Exception {
        mockMvc.perform(post("/api/utenti/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(PASSWORD_VALIDA)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(keycloakAdminClient);
    }

    // --- validazione -------------------------------------------------------

    @Test
    void unaPasswordTroppoCortaVieneRifiutataPrimaDiChiamareKeycloak() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa(SUB_UTENTE_A, Duration.ofSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("corta1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.nuovaPassword").exists());

        verify(keycloakAdminClient, never()).impostaPassword(anyString(), anyString(), anyString());
    }

    @Test
    void unaPasswordSenzaCifreVieneRifiutata() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa(SUB_UTENTE_A, Duration.ofSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("PasswordSenzaNumeri")))
                .andExpect(status().isBadRequest());

        verify(keycloakAdminClient, never()).impostaPassword(anyString(), anyString(), anyString());
    }

    @Test
    void unCampoEstraneoNelPayloadFaFallireLaRichiesta() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa(SUB_UTENTE_A, Duration.ofSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"" + PASSWORD_VALIDA + "\",\"keycloakId\":\"altro\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(keycloakAdminClient);
    }

    // --- errori lato Keycloak ----------------------------------------------

    @Test
    void unaPasswordRifiutataDallaPolicyDelRealmDiventaUn400() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        doThrow(new PasswordNonConformeException("La password non rispetta i requisiti richiesti"))
                .when(keycloakAdminClient).impostaPassword(anyString(), anyString(), anyString());

        MvcResult risultato = mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa(SUB_UTENTE_A, Duration.ofSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(PASSWORD_VALIDA)))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
        verify(keycloakAdminClient, never()).terminaSessioniSenzaPropagareErrori(anyString(), anyString());
    }

    @Test
    void seKeycloakNonRispondeLOperazioneFallisceCon503() throws Exception {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        doThrow(new IdentityProviderNonDisponibileException("Keycloak non raggiungibile"))
                .when(keycloakAdminClient).impostaPassword(anyString(), anyString(), anyString());

        MvcResult risultato = mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa(SUB_UTENTE_A, Duration.ofSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(PASSWORD_VALIDA)))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(risultato.getResponse().getContentAsString()).doesNotContain("Keycloak");
    }

    @Test
    void unUtenteSenzaRecordLocaleNonPuoCambiareLaPassword() throws Exception {
        // token valido e recente, ma nessun utente locale: il keycloakId su cui agire
        // arriverebbe da nulla
        mockMvc.perform(post("/api/utenti/me/password")
                        .with(tokenAutenticatoDa("sub-sconosciuto", Duration.ofSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(PASSWORD_VALIDA)))
                .andExpect(status().isNotFound());

        verify(keycloakAdminClient, never()).impostaPassword(anyString(), anyString(), anyString());
    }
}
