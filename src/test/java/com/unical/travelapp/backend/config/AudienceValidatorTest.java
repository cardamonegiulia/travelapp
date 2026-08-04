package com.unical.travelapp.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("travelapp-backend");

    private Jwt tokenConAudience(String... audience) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("keycloak-user-id")
                .audience(java.util.List.of(audience))
                .build();
    }

    @Test
    void accettaTokenConAudienceAttesa() {
        OAuth2TokenValidatorResult result = validator.validate(tokenConAudience("travelapp-backend"));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rifiutaTokenConAudienceDiUnAltroClient() {
        OAuth2TokenValidatorResult result = validator.validate(tokenConAudience("altro-client"));
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void rifiutaTokenSenzaAudience() {
        OAuth2TokenValidatorResult result = validator.validate(tokenConAudience());
        assertThat(result.hasErrors()).isTrue();
    }
}
