package com.unical.travelapp.backend.identity.security;

import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtenteSecurityTest {

    @Mock
    private UtenteService utenteService;

    @InjectMocks
    private UtenteSecurity utenteSecurity;

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("keycloak-user-id")
                .build();
    }

    @Test
    void ritornaTruePerLaPropriaRisorsa() {
        Utente utente = new Utente();
        utente.setId(42L);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt());
        when(utenteService.ottieniUtenteDaToken(any(Jwt.class))).thenReturn(utente);

        assertThat(utenteSecurity.isSelf(42L, authentication)).isTrue();
    }

    @Test
    void ritornaFalsePerLaRisorsaDiUnAltroUtente() {
        Utente utente = new Utente();
        utente.setId(42L);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt());
        when(utenteService.ottieniUtenteDaToken(any(Jwt.class))).thenReturn(utente);

        assertThat(utenteSecurity.isSelf(99L, authentication)).isFalse();
    }
}
