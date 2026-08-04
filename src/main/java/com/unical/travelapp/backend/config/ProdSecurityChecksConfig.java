package com.unical.travelapp.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// Controlli di sicurezza attivi SOLO con il profilo "prod": se l'issuer Keycloak non e' HTTPS
// l'avvio fallisce subito, invece di partire silenziosamente con un resource server che fida
// di un endpoint non cifrato.
@Configuration
@Profile("prod")
public class ProdSecurityChecksConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @PostConstruct
    public void verificaIssuerHttps() {
        if (issuerUri == null || !issuerUri.startsWith("https://")) {
            throw new IllegalStateException(
                    "In produzione l'issuer-uri OAuth2 deve essere HTTPS. Valore attuale: " + issuerUri);
        }
    }
}
