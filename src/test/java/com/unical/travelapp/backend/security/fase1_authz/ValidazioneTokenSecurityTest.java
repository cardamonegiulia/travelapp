package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.config.AudienceValidator;
import com.unical.travelapp.backend.security.support.RsaTokenFactory;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fase 1 - validazione del token vera e propria: firma, scadenza, issuer, audience.
 *
 * <p>Il post-processor {@code jwt()} usato altrove scavalca il JwtDecoder, quindi questi
 * controlli si collaudano qui: chiavi RSA generate a runtime, token firmati con Nimbus e
 * la stessa catena di validator montata da SecurityConfig
 * ({@code JwtValidators.createDefaultWithIssuer} + {@link AudienceValidator}).
 *
 * <p>Nessun Keycloak reale e nessun token vero: i test restano verdi anche con il realm
 * non ancora configurato.
 */
class ValidazioneTokenSecurityTest {

    private static final String ISSUER_ATTESO = "http://travelapp-keycloak:8080/realms/travelapp";
    private static final String AUDIENCE_ATTESA = "travelapp-backend";

    private final RsaTokenFactory idp = new RsaTokenFactory();

    /** Ricostruisce esattamente la catena di validazione di SecurityConfig#jwtDecoder. */
    private JwtDecoder decoderComeInProduzione() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(idp.chiavePubblica()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
                JwtValidators.createDefaultWithIssuer(ISSUER_ATTESO),
                new AudienceValidator(AUDIENCE_ATTESA))));
        return decoder;
    }

    // --- audience -----------------------------------------------------------

    @Test
    void accettaIlTokenConLAudienceAttesa() {
        String token = idp.token(ISSUER_ATTESO, List.of(AUDIENCE_ATTESA), "utente-1", Map.of());

        Jwt decodificato = decoderComeInProduzione().decode(token);

        assertThat(decodificato.getSubject()).isEqualTo("utente-1");
    }

    @Test
    void rifiutaIlTokenConAudienceAccount() {
        // e' esattamente lo stato attuale del realm: i token reali hanno aud=account.
        // Finche' il mapper di audience non e' configurato, l'API deve rifiutarli.
        String token = idp.token(ISSUER_ATTESO, List.of("account"), "utente-1", Map.of());

        assertThatThrownBy(() -> decoderComeInProduzione().decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("audience");
    }

    @Test
    void accettaIlTokenSeLAudienceAttesaEUnaDelleTante() {
        String token = idp.token(ISSUER_ATTESO, List.of("account", AUDIENCE_ATTESA, "altro"), "utente-1", Map.of());

        assertThat(decoderComeInProduzione().decode(token).getSubject()).isEqualTo("utente-1");
    }

    @Test
    void rifiutaIlTokenSenzaAudience() {
        String token = idp.token(ISSUER_ATTESO, null, "utente-1", Map.of());

        assertThatThrownBy(() -> decoderComeInProduzione().decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void senzaAudienceValidatorLoStessoTokenPasserebbe() {
        // dimostra che e' proprio l'AudienceValidator a bloccare aud=account:
        // togliendolo dalla catena il token viene accettato. Se il validator sparisse dalla
        // configurazione, il test "rifiutaIlTokenConAudienceAccount" diventerebbe rosso.
        NimbusJwtDecoder senzaAudience = NimbusJwtDecoder.withPublicKey(idp.chiavePubblica()).build();
        senzaAudience.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER_ATTESO));

        String token = idp.token(ISSUER_ATTESO, List.of("account"), "utente-1", Map.of());

        assertThat(senzaAudience.decode(token).getSubject()).isEqualTo("utente-1");
    }

    // --- issuer -------------------------------------------------------------

    @ParameterizedTest(name = "issuer {0} -> rifiutato")
    @ValueSource(strings = {
            "http://localhost:8090/realms/travelapp",
            "https://idp-malevolo.example/realms/travelapp",
            "http://travelapp-keycloak:8080/realms/altro-realm"
    })
    void rifiutaIlTokenConIssuerDiverso(String issuerDelToken) {
        // caso concreto del progetto: localhost:8090 (accesso dall'host) contro
        // travelapp-keycloak:8080 (nome del servizio in docker compose). Se l'issuer
        // configurato e quello del token non coincidono il token va rifiutato.
        String token = idp.token(issuerDelToken, List.of(AUDIENCE_ATTESA), "utente-1", Map.of());

        assertThatThrownBy(() -> decoderComeInProduzione().decode(token))
                .isInstanceOf(JwtException.class);
    }

    // --- firma e scadenza ---------------------------------------------------

    @Test
    void rifiutaIlTokenScaduto() {
        String token = idp.tokenScaduto(ISSUER_ATTESO, List.of(AUDIENCE_ATTESA), "utente-1");

        assertThatThrownBy(() -> decoderComeInProduzione().decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rifiutaIlTokenFirmatoConUnaChiaveDiversa() {
        String token = idp.tokenConFirmaSbagliata(ISSUER_ATTESO, List.of(AUDIENCE_ATTESA), "utente-1");

        assertThatThrownBy(() -> decoderComeInProduzione().decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rifiutaUnTokenNonFirmato() {
        // "alg: none": header e payload in chiaro, nessuna firma
        String none = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "." + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"iss\":\"" + ISSUER_ATTESO + "\",\"aud\":\"" + AUDIENCE_ATTESA
                        + "\",\"sub\":\"attaccante\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + ".";

        assertThatThrownBy(() -> decoderComeInProduzione().decode(none))
                .isInstanceOf(JwtException.class);
    }

    // --- coerenza fra i validator e il resto della configurazione -----------

    @Test
    void ilValidatorDiAudienceUsaEsattamenteLaPropertyConfigurata() {
        OAuth2TokenValidator<Jwt> validator = new AudienceValidator(TestJwt.CLIENT_ID);

        Jwt conAudienceGiusta = Jwt.withTokenValue("t").header("alg", "RS256")
                .audience(List.of(TestJwt.CLIENT_ID)).subject("s").build();
        Jwt conAudienceSbagliata = Jwt.withTokenValue("t").header("alg", "RS256")
                .audience(List.of("account")).subject("s").build();

        assertThat(validator.validate(conAudienceGiusta).hasErrors()).isFalse();
        assertThat(validator.validate(conAudienceSbagliata).hasErrors()).isTrue();
    }
}
