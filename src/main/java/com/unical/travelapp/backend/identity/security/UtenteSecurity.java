package com.unical.travelapp.backend.identity.security;

import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("utenteSecurity")
@RequiredArgsConstructor
public class UtenteSecurity {

    private final UtenteService utenteService;

    public boolean isSelf(Long id, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Utente utente = utenteService.ottieniUtenteDaToken(jwt);
        return utente.getId().equals(id);
    }
}
