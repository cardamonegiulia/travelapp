package com.unical.travelapp.backend.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

// L'"autore" per Spring Data JPA Auditing e' il subject (sub) del token, mai un nome
// fornito dal client: coerente con "l'identita' va sempre presa dal token".
@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(SYSTEM);
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.ofNullable(jwt.getSubject()).or(() -> Optional.of(SYSTEM));
        }
        return Optional.of(authentication.getName());
    }
}
