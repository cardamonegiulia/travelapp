package com.unical.travelapp.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter("travelapp-backend");

    private Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("keycloak-user-id");
    }

    @Test
    void estraeIRuoliRealmDalClaimAnnidatoRealmAccess() {
        Jwt jwt = baseJwt()
                .claim("realm_access", Map.of("roles", List.of("VIAGGIATORE", "ADMIN")))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_VIAGGIATORE", "ROLE_ADMIN");
    }

    @Test
    void estraeIRuoliClientDalClaimAnnidatoResourceAccess() {
        Jwt jwt = baseJwt()
                .claim("resource_access", Map.of(
                        "travelapp-backend", Map.of("roles", List.of("ORGANIZZATORE"))
                ))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ORGANIZZATORE");
    }

    @Test
    void ignoraRuoliClientDiClientDiversiDalResourceServer() {
        Jwt jwt = baseJwt()
                .claim("resource_access", Map.of(
                        "altro-client", Map.of("roles", List.of("ADMIN"))
                ))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void nessunClaimRuoliProduceAutoritaVuote() {
        Jwt jwt = baseJwt().claim("email", "user@example.com").build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
