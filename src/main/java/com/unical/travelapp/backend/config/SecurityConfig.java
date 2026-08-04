package com.unical.travelapp.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.security.expected-audience}")
    private String expectedAudience;

    @Value("${app.security.resource-client-id}")
    private String resourceClientId;

    @Value("${app.security.require-https:false}")
    private boolean requireHttps;

    @Value("${app.security.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter) throws Exception {
        http
                // API stateless a bearer token (Authorization: Bearer <jwt>), nessun cookie di sessione:
                // il CSRF classico sfrutta l'invio automatico dei cookie dal browser, qui non si applica.
                // Se in futuro si introducesse un flusso basato su cookie di sessione, va riabilitato.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        // 'unsafe-inline' e' necessario solo per gli asset bundled di springdoc/swagger-ui;
                        // le risposte JSON dell'API non eseguono script quindi la CSP e' qui difesa in profondita'.
                        // In produzione swagger-ui e' comunque disabilitato (vedi application-prod.properties).
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'; " +
                                        "style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'"
                        ))
                )
                .authorizeHttpRequests(auth -> auth
                        // rotte pubbliche
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // actuator (se mai abilitato) riservato agli admin
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // API: autenticazione minima richiesta, i ruoli fini sono su @PreAuthorize
                        .requestMatchers("/api/**").authenticated()
                        // deny-by-default: qualunque rotta non mappata sopra viene negata
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                // dopo l'autenticazione: qui il claim "sub" del JWT e' gia' disponibile per la chiave del rate limit
                .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class);

        // HTTPS obbligatorio solo quando esplicitamente richiesto (profilo prod): in sviluppo
        // locale Keycloak gira su HTTP, forzare HTTPS romperebbe l'ambiente di dev.
        if (requireHttps) {
            http.redirectToHttps(Customizer.withDefaults());
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Bearer token nell'header Authorization, non cookie: nessuna credenziale cross-origin da consentire.
        // Mai combinare allowCredentials=true con un allow-list permissiva o con "*".
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(expectedAudience);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(withIssuer, withAudience)));

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter(resourceClientId));
        return converter;
    }
}
