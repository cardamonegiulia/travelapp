package com.unical.travelapp.backend.security.support;

import com.unical.travelapp.backend.config.KeycloakRoleConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Costruttori di token per i test di AUTORIZZAZIONE (matrice ruoli, BOLA).
 *
 * <p>Il post-processor {@code jwt()} scavalca il JwtDecoder, quindi qui NON si verificano
 * firma, audience e issuer: quelli hanno test dedicati che costruiscono e firmano token
 * veri con chiavi RSA generate a runtime (vedi {@link RsaTokenFactory}).
 *
 * <p>Le authority sono comunque ricavate dal {@link KeycloakRoleConverter} di produzione a
 * partire dai claim Keycloak, non inventate: se il converter smettesse di leggere
 * {@code realm_access}/{@code resource_access} tutti i test di ruolo diventerebbero rossi.
 */
public final class TestJwt {

    public static final String CLIENT_ID = "travelapp-backend";
    public static final String ISSUER = "https://idp.test.local/realms/travelapp";

    private TestJwt() {
    }

    /** Token con ruoli nel claim realm_access.roles (il caso standard di Keycloak). */
    public static JwtRequestPostProcessor conRuoliRealm(String subject, String... ruoli) {
        return costruisci(builder -> builder
                .claim("preferred_username", subject)
                .claim("realm_access", Map.of("roles", List.of(ruoli))), subject);
    }

    /** Token con ruoli nel claim resource_access.&lt;client-id&gt;.roles (ruoli client). */
    public static JwtRequestPostProcessor conRuoliClient(String subject, String... ruoli) {
        return costruisci(builder -> builder
                .claim("preferred_username", subject)
                .claim("resource_access", Map.of(CLIENT_ID, Map.of("roles", List.of(ruoli)))), subject);
    }

    /** Token autenticato ma senza alcun ruolo applicativo (lo stato attuale del realm). */
    public static JwtRequestPostProcessor senzaRuoli(String subject) {
        return costruisci(builder -> builder.claim("preferred_username", subject), subject);
    }

    /** Stesso subject ma username/email diversi: l'ownership non deve cambiare comportamento. */
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
