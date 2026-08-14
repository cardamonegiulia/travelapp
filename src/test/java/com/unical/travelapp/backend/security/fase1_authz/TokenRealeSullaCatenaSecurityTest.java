package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.RsaTokenFactory;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.ServerJwkDiProva;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 1 - token JWT veri (firmati con RSA a runtime) attraverso l'intera catena di filtri.
 *
 * <p>E' il test che chiude il cerchio: gli altri test di autorizzazione usano il
 * post-processor {@code jwt()}, che scavalca il decoder. Qui il token viaggia
 * nell'header {@code Authorization: Bearer ...} e viene decodificato dal JwtDecoder di
 * produzione, che scarica davvero le chiavi pubbliche e verifica davvero firma, issuer,
 * audience e scadenza. L'unica cosa sostituita e' l'indirizzo della JWK set, che punta a
 * un server locale invece che a Keycloak (vedi {@link ServerJwkDiProva}): nessun controllo
 * viene disattivato.
 *
 * <p>Un token non valido deve produrre 401, mai 200 e mai 500.
 */
class TokenRealeSullaCatenaSecurityTest extends SecurityIntegrationTestBase {

    private static final String ISSUER = TestJwt.ISSUER;
    private static final String AUDIENCE = TestJwt.CLIENT_ID;

    @DynamicPropertySource
    static void chiaviPubblicheLocali(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> ServerJwkDiProva.istanza().jwkSetUri());
    }

    private static RsaTokenFactory idp() {
        return ServerJwkDiProva.istanza().idp();
    }

    @BeforeEach
    void utenteLocale() {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void unTokenValidoAttraversaLaCatenaEAutorizza() throws Exception {
        String token = idp().token(ISSUER, List.of(AUDIENCE), SUB_UTENTE_A,
                Map.of("realm_access", Map.of("roles", List.of("VIAGGIATORE")),
                        "preferred_username", "utente.a"));

        mockMvc.perform(get("/api/itinerari").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void iRuoliDelTokenFirmatoDiventanoAuthorityReali() throws Exception {
        utente(SUB_ADMIN, Ruolo.ADMIN);
        String tokenAdmin = idp().token(ISSUER, List.of(AUDIENCE), SUB_ADMIN,
                Map.of("realm_access", Map.of("roles", List.of("ADMIN"))));
        String tokenViaggiatore = idp().token(ISSUER, List.of(AUDIENCE), SUB_UTENTE_A,
                Map.of("realm_access", Map.of("roles", List.of("VIAGGIATORE"))));

        mockMvc.perform(get("/api/utenti").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/utenti").header("Authorization", bearer(tokenViaggiatore)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unTokenConAudienceAccountVieneRespintoCon401() throws Exception {
        // stato attuale del realm: i token reali hanno aud=account
        String token = idp().token(ISSUER, List.of("account"), SUB_UTENTE_A,
                Map.of("realm_access", Map.of("roles", List.of("VIAGGIATORE"))));

        MvcResult risultato = mockMvc.perform(get("/api/itinerari").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        NessunLeak.verifica(risultato);
    }

    @Test
    void unTokenSenzaClaimAudVieneRespintoCon401() throws Exception {
        // token senza alcun claim "aud": deve essere rifiutato in modo pulito (401),
        // non far esplodere il validator con un errore non gestito.
        String token = idp().token(ISSUER, null, SUB_UTENTE_A,
                Map.of("realm_access", Map.of("roles", List.of("VIAGGIATORE"))));

        MvcResult risultato = mockMvc.perform(get("/api/itinerari").header("Authorization", bearer(token)))
                .andReturn();

        assertThat(risultato.getResponse().getStatus())
                .as("un token senza audience non deve produrre un errore interno")
                .isEqualTo(401);
        NessunLeak.verifica(risultato);
    }

    @Test
    void unTokenConIssuerSbagliatoVieneRespintoCon401() throws Exception {
        String token = idp().token(
                "http://localhost:8090/realms/travelapp", List.of(AUDIENCE), SUB_UTENTE_A, Map.of());

        mockMvc.perform(get("/api/itinerari").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenScadutoVieneRespintoCon401() throws Exception {
        String token = idp().tokenScaduto(ISSUER, List.of(AUDIENCE), SUB_UTENTE_A);

        mockMvc.perform(get("/api/itinerari").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFirmatoDaUnAltraChiaveVieneRespintoCon401() throws Exception {
        String token = idp()
                .tokenConFirmaSbagliata(ISSUER, List.of(AUDIENCE), SUB_UTENTE_A);

        MvcResult risultato = mockMvc.perform(get("/api/itinerari").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        NessunLeak.verifica(risultato);
    }

    @Test
    void ilTokenNonValidoNonCreaNeLeggeDatiApplicativi() throws Exception {
        String token = idp().token(ISSUER, List.of("account"), "sub-sconosciuto",
                Map.of("realm_access", Map.of("roles", List.of("ADMIN"))));

        mockMvc.perform(get("/api/utenti").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());

        assertThat(utenteRepository.findByKeycloakId("sub-sconosciuto"))
                .as("un token respinto non deve lasciare traccia nel dominio applicativo")
                .isEmpty();
    }
}
