package com.unical.travelapp.backend.common.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

    @AfterEach
    void pulisciContesto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ritornaIlSubjectDelTokenPerUnUtenteAutenticato() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("keycloak-sub-42")
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(auditorAware.getCurrentAuditor()).contains("keycloak-sub-42");
    }

    @Test
    void ritornaSystemSenzaAutenticazione() {
        assertThat(auditorAware.getCurrentAuditor()).contains("system");
    }
}
