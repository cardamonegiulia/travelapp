package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.config.KeycloakRoleConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class KeycloakRoleConverterFailClosedTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter("travelapp-backend");

    private Jwt.Builder tokenBase() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("keycloak-sub");
    }

    @Test
    void ruoliRealmERuoliClientSiSommano() {
        Jwt jwt = tokenBase()
                .claim("realm_access", Map.of("roles", List.of("VIAGGIATORE")))
                .claim("resource_access", Map.of("travelapp-backend", Map.of("roles", List.of("ORGANIZZATORE"))))
                .build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_VIAGGIATORE", "ROLE_ORGANIZZATORE");
    }

    @Test
    void ilPrefissoRoleVieneApplicatoUnaVoltaSola() {
        Jwt jwt = tokenBase().claim("realm_access", Map.of("roles", List.of("ADMIN"))).build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN")
                .noneMatch(authority -> authority.startsWith("ROLE_ROLE_"));
    }

    @Test
    void claimDeiRuoliAssenteNonConcedeNulla() {
        assertThat(converter.convert(tokenBase().build())).isEmpty();
    }

    @Test
    void claimDeiRuoliNulloNonConcedeNullaENonEsplode() {
        Map<String, Object> conNull = new HashMap<>();
        conNull.put("roles", null);
        Jwt jwt = tokenBase().claim("realm_access", conNull).build();

        assertThatCode(() -> converter.convert(jwt)).doesNotThrowAnyException();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void claimDeiRuoliDiTipoSbagliatoNonConcedeNullaENonEsplode() {
        Jwt stringa = tokenBase().claim("realm_access", Map.of("roles", "ADMIN")).build();
        Jwt numero = tokenBase().claim("realm_access", Map.of("roles", 42)).build();
        Jwt mappa = tokenBase().claim("realm_access", Map.of("roles", Map.of("a", "ADMIN"))).build();

        for (Jwt jwt : List.of(stringa, numero, mappa)) {
            assertThatCode(() -> converter.convert(jwt)).doesNotThrowAnyException();
            assertThat(converter.convert(jwt)).isEmpty();
        }
    }

    @Test
    void resourceAccessDiTipoSbagliatoNonConcedeNulla() {
        Jwt jwt = tokenBase().claim("resource_access", Map.of("travelapp-backend", "ADMIN")).build();

        assertThatCode(() -> converter.convert(jwt)).doesNotThrowAnyException();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void listaDiRuoliVuotaNonConcedeNulla() {
        Jwt jwt = tokenBase().claim("realm_access", Map.of("roles", List.of())).build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void iRuoliDiUnAltroClientVengonoIgnorati() {
        Jwt jwt = tokenBase()
                .claim("resource_access", Map.of("un-altro-client", Map.of("roles", List.of("ADMIN"))))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void ilClaimScopeNonDiventaMaiUnaAuthority() {
        Jwt jwt = tokenBase()
                .claim("scope", "openid profile email write:viaggi admin")
                .claim("scp", List.of("admin", "write:viaggi"))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void unClaimArbitrarioDelTokenNonDiventaUnRuolo() {
        Jwt jwt = tokenBase()
                .claim("roles", List.of("ADMIN"))
                .claim("authorities", List.of("ROLE_ADMIN"))
                .claim("groups", List.of("/admins"))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
