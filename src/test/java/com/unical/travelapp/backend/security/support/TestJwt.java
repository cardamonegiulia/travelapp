package com.unical.travelapp.backend.security.support;

import com.unical.travelapp.backend.config.KeycloakRoleConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestJwt {

    public static final String CLIENT_ID = "travelapp-backend";
    public static final String ISSUER = "https://idp.test.local/realms/travelapp";

    private TestJwt() {
    }

    public static JwtRequestPostProcessor conRuoliRealm(String subject, String... ruoli) {
        return costruisci(builder -> builder
                .claim("preferred_username", subject)
                .claim("realm_access", Map.of("roles", List.of(ruoli))), subject);
    }

    public static JwtRequestPostProcessor conRuoliClient(String subject, String... ruoli) {
        return costruisci(builder -> builder
                .claim("preferred_username", subject)
                .claim("resource_access", Map.of(CLIENT_ID, Map.of("roles", List.of(ruoli)))), subject);
    }

    public static JwtRequestPostProcessor senzaRuoli(String subject) {
        return costruisci(builder -> builder.claim("preferred_username", subject), subject);
    }

    public static JwtRequestPostProcessor conEmail(String subject, String email, String... ruoli) {
        return costruisci(builder -> builder
                .claim("preferred_username", email)
                .claim("email", email)
                .claim("realm_access", Map.of("roles", List.of(ruoli))), subject);
    }

    public static JwtRequestPostProcessor conUsernameDiverso(String subject, String username, String... ruoli) {
        return costruisci(builder -> builder
                .claim("preferred_username", username)
                .claim("email", username + "@altro.test")
                .claim("realm_access", Map.of("roles", List.of(ruoli))), subject);
    }

    private static JwtRequestPostProcessor costruisci(Consumer<Jwt.Builder> claims, String subject) {
        return jwt()
                .jwt(builder -> {
                    builder.issuer(ISSUER)
                            .audience(List.of(CLIENT_ID))
                            .subject(subject);
                    claims.accept(builder);
                })
                .authorities(new KeycloakRoleConverter(CLIENT_ID));
    }
}
