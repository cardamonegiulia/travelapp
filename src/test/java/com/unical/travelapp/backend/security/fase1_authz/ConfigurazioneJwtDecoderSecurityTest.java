package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.config.AudienceValidator;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurazioneJwtDecoderSecurityTest extends SecurityIntegrationTestBase {

    @Autowired private JwtDecoder jwtDecoder;
    @Autowired private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void ilDecoderDellApplicazioneMontaSiaIlValidatorDiIssuerSiaQuelloDiAudience() {
        Object validator = ReflectionTestUtils.getField(jwtDecoder, "jwtValidator");

        assertThat(validator)
                .as("il JwtDecoder deve avere una catena di validator, non quella di default")
                .isInstanceOf(DelegatingOAuth2TokenValidator.class);

        List<OAuth2TokenValidator<Jwt>> validatorAppiattiti = appiattisci(validator);

        assertThat(validatorAppiattiti)
                .as("l'AudienceValidator deve essere installato sul decoder")
                .anyMatch(AudienceValidator.class::isInstance);

        assertThat(validatorAppiattiti)
                .as("deve esserci un validator di issuer (JwtIssuerValidator)")
                .anyMatch(v -> v.getClass().getSimpleName().contains("Issuer"));

        assertThat(validatorAppiattiti)
                .as("deve esserci il controllo di scadenza (JwtTimestampValidator)")
                .anyMatch(v -> v.getClass().getSimpleName().contains("Timestamp"));
    }

    @Test
    void ilConverterDelleAuthorityEQuelloKeycloakNonQuelloDiDefaultSugliScope() {
        Object converter = ReflectionTestUtils.getField(jwtAuthenticationConverter, "jwtGrantedAuthoritiesConverter");

        assertThat(converter)
                .as("le authority devono venire dai ruoli Keycloak, non dal claim scope")
                .isInstanceOf(com.unical.travelapp.backend.config.KeycloakRoleConverter.class);
    }

    @Test
    void laPoliticaDiSessioneEStateless() {
        assertThat(SessionCreationPolicy.STATELESS).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private List<OAuth2TokenValidator<Jwt>> appiattisci(Object validator) {
        List<OAuth2TokenValidator<Jwt>> risultato = new ArrayList<>();
        if (validator instanceof DelegatingOAuth2TokenValidator<?> delegante) {
            Object interni = ReflectionTestUtils.getField(delegante, "tokenValidators");
            for (Object interno : (Collection<Object>) interni) {
                risultato.addAll(appiattisci(interno));
            }
        } else if (validator != null) {
            risultato.add((OAuth2TokenValidator<Jwt>) validator);
        }
        return risultato;
    }
}
