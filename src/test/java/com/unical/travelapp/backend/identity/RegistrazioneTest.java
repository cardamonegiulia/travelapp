package com.unical.travelapp.backend.identity;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.RegistrazioneNonDisponibileException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.NuovoUtenteKeycloak;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.RuoloRealm;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registrazione self-service su {@code POST /api/auth/registrazione}.
 *
 * <p>{@link KeycloakAdminClient} e' l'unico collaboratore sostituito da un mock: i test devono
 * poter verificare <b>cosa</b> viene chiesto a Keycloak (quale ruolo, quale compensazione)
 * senza dipendere da un'istanza reale. Tutto il resto - catena di filtri, validazione,
 * mapping degli errori, persistenza - e' quello di produzione.
 */
class RegistrazioneTest extends SecurityIntegrationTestBase {

    private static final String TOKEN_ADMIN = "token-service-account";
    private static final String KEYCLOAK_ID = "b1f0c5a2-0000-4000-8000-000000000001";
    private static final String PASSWORD_VALIDA = "PasswordSicura1";

    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void keycloakRisponde() {
        when(keycloakAdminClient.ottieniTokenAmministrativo()).thenReturn(TOKEN_ADMIN);
        when(keycloakAdminClient.trovaRuoloRealm(anyString(), anyString()))
                .thenAnswer(invocazione -> {
                    String nome = invocazione.getArgument(1);
                    return Optional.of(new RuoloRealm("id-" + nome.toLowerCase(), nome));
                });
        when(keycloakAdminClient.creaUtente(anyString(), any())).thenReturn(KEYCLOAK_ID);
    }

    private String payload(String email, String ruolo) {
        return payload(email, ruolo, PASSWORD_VALIDA);
    }

    private String payload(String email, String ruolo, String password) {
        return """
                {"nome":"Mario","cognome":"Rossi","email":"%s","password":"%s","ruolo":"%s"}
                """.formatted(email, password, ruolo);
    }

    // --- percorso felice ---------------------------------------------------

    @ParameterizedTest(name = "registrazione come {0}")
    @ValueSource(strings = {"VIAGGIATORE", "ORGANIZZATORE"})
    void laRegistrazioneCreaUtenteSuKeycloakConIlRuoloScelto(String ruolo) throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("mario.rossi@example.test", ruolo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("mario.rossi@example.test"))
                .andExpect(jsonPath("$.ruolo").value(ruolo));

        ArgumentCaptor<RuoloRealm> ruoloAssegnato = ArgumentCaptor.forClass(RuoloRealm.class);
        verify(keycloakAdminClient).assegnaRuoloRealm(eq(TOKEN_ADMIN), eq(KEYCLOAK_ID), ruoloAssegnato.capture());
        assertThat(ruoloAssegnato.getValue().nome())
                .as("il ruolo realm assegnato deve essere quello scelto dall'utente")
                .isEqualTo(ruolo);
    }

    @Test
    void ilRuoloLocaleCoincideConQuelloAssegnatoSuKeycloak() throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("org@example.test", "ORGANIZZATORE")))
                .andExpect(status().isCreated());

        Utente salvato = utenteRepository.findByKeycloakId(KEYCLOAK_ID).orElseThrow();
        assertThat(salvato.getRuolo())
                .as("il campo utenti.ruolo non deve piu' essere un default fisso")
                .isEqualTo(Ruolo.ORGANIZZATORE);
        assertThat(salvato.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
    }

    @Test
    void lEmailVieneNormalizzataPrimaDiArrivareAKeycloak() throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("Mario.Rossi@Example.TEST", "VIAGGIATORE")))
                .andExpect(status().isCreated());

        ArgumentCaptor<NuovoUtenteKeycloak> nuovo = ArgumentCaptor.forClass(NuovoUtenteKeycloak.class);
        verify(keycloakAdminClient).creaUtente(eq(TOKEN_ADMIN), nuovo.capture());
        assertThat(nuovo.getValue().username()).isEqualTo("mario.rossi@example.test");
        assertThat(nuovo.getValue().email()).isEqualTo("mario.rossi@example.test");
    }

    @Test
    void laRispostaNonRimandaIndietroLaPasswordNeIlKeycloakId() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("mario.rossi@example.test", "VIAGGIATORE")))
                .andExpect(status().isCreated())
                .andReturn();

        String body = risultato.getResponse().getContentAsString();
        assertThat(body).doesNotContain(PASSWORD_VALIDA);
        assertThat(body)
                .as("il keycloakId e' un identificativo interno e non compare nelle risposte")
                .doesNotContain(KEYCLOAK_ID);
    }

    // --- il ruolo ADMIN non e' auto-assegnabile ----------------------------

    @Test
    void ilRuoloAdminNonEAccettatoInRegistrazione() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("aspirante.admin@example.test", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.ruolo").exists())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(utenteRepository.count()).isZero();
        verifyNoInteractions(keycloakAdminClient);
    }

    @ParameterizedTest(name = "ruolo non ammesso: {0}")
    @ValueSource(strings = {"admin", "Admin", "ROLE_ADMIN", "realm-admin", ""})
    void nessunaVarianteDiAdminVieneAccettata(String ruolo) throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("tentativo@example.test", ruolo)))
                .andExpect(status().isBadRequest());

        assertThat(utenteRepository.count()).isZero();
        verifyNoInteractions(keycloakAdminClient);
    }

    // --- validazione dell'input --------------------------------------------

    @ParameterizedTest(name = "password rifiutata: \"{0}\"")
    @ValueSource(strings = {"corta1", "soltantolettere", "123456789012345", "            "})
    void unaPasswordDeboleVieneRifiutata(String password) throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("mario.rossi@example.test", "VIAGGIATORE", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.password").exists());

        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void unEmailMalformataVieneRifiutata() throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("non-una-email", "VIAGGIATORE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.email").exists());

        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void unCampoDiSistemaNelPayloadFaFallireLaRichiesta() throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Mario","cognome":"Rossi","email":"mario.rossi@example.test",
                                 "password":"%s","ruolo":"VIAGGIATORE","keycloakId":"scelto-da-me","id":99}
                                """.formatted(PASSWORD_VALIDA)))
                .andExpect(status().isBadRequest())
                .andReturn();

        NessunLeak.verifica(risultato);
        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void unEmailGiaRegistrataLocalmenteNonArrivaAKeycloak() throws Exception {
        Utente esistente = utente("sub-esistente", Ruolo.VIAGGIATORE);

        MvcResult risultato = mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(esistente.getEmail(), "VIAGGIATORE")))
                .andExpect(status().isConflict())
                .andReturn();

        NessunLeak.verifica(risultato);
        verifyNoInteractions(keycloakAdminClient);
        assertThat(utenteRepository.count()).isEqualTo(1);
    }

    // --- errori lato Keycloak ----------------------------------------------

    @Test
    void seIlRuoloRealmNonEsisteLUtenteNonVieneNemmenoCreato() throws Exception {
        when(keycloakAdminClient.trovaRuoloRealm(anyString(), eq("VIAGGIATORE"))).thenReturn(Optional.empty());

        MvcResult risultato = mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("mario.rossi@example.test", "VIAGGIATORE")))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        NessunLeak.verifica(risultato);
        verify(keycloakAdminClient, never()).creaUtente(anyString(), any());
        assertThat(utenteRepository.count())
                .as("nessun account a meta': ne' su Keycloak ne' in locale")
                .isZero();
    }

    @Test
    void seLAssegnazioneDelRuoloFallisceLUtenteKeycloakVieneRimosso() throws Exception {
        doThrow(new RegistrazioneNonDisponibileException("assegnazione fallita"))
                .when(keycloakAdminClient).assegnaRuoloRealm(anyString(), anyString(), any());

        MvcResult risultato = mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("mario.rossi@example.test", "VIAGGIATORE")))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        NessunLeak.verifica(risultato);
        verify(keycloakAdminClient, times(1)).eliminaUtenteSenzaPropagareErrori(TOKEN_ADMIN, KEYCLOAK_ID);
        assertThat(utenteRepository.count())
                .as("nessun record locale se il ruolo non e' stato assegnato")
                .isZero();
    }

    @Test
    void seKeycloakNonRispondeLaRegistrazioneRestituisce503() throws Exception {
        when(keycloakAdminClient.ottieniTokenAmministrativo())
                .thenThrow(new RegistrazioneNonDisponibileException("Keycloak non raggiungibile"));

        MvcResult risultato = mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("mario.rossi@example.test", "VIAGGIATORE")))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        NessunLeak.verifica(risultato);
        assertThat(risultato.getResponse().getContentAsString())
                .as("il motivo tecnico resta nei log, non nel body")
                .doesNotContain("Keycloak");
        assertThat(utenteRepository.count()).isZero();
    }

    // --- esposizione della rotta -------------------------------------------

    @Test
    void laRegistrazioneEPubblicaEnonRichiedeToken() throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("senza.token@example.test", "VIAGGIATORE")))
                .andExpect(status().isCreated());
    }

    @Test
    void soloIlPostEPubblicoSullaRottaDiRegistrazione() throws Exception {
        mockMvc.perform(get("/api/auth/registrazione"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ancheConUnTokenLaRegistrazioneNonCambiaComportamento() throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                        .with(TestJwt.conRuoliRealm("sub-qualsiasi", "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("con.token@example.test", "ORGANIZZATORE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruolo").value("ORGANIZZATORE"));
    }
}
