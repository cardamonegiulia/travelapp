package com.unical.travelapp.backend.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unical.travelapp.backend.config.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Audit log applicativo per le operazioni critiche: logger dedicato ("AUDIT"), separato
// dal log applicativo (vedi logback-spring.xml, scrive su file proprio). Non logga MAI il
// token JWT completo, password o header Authorization: solo subject/username estratti dal
// token gia' validato.
@Component
public class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final String SISTEMA = "system";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public enum Esito { SUCCESS, FAILURE }

    public void success(String azione, String risorsaTipo, String risorsaId) {
        scrivi(azione, risorsaTipo, risorsaId, Esito.SUCCESS, null);
    }

    public void failure(String azione, String risorsaTipo, String risorsaId, String motivo) {
        scrivi(azione, risorsaTipo, risorsaId, Esito.FAILURE, motivo);
    }

    private void scrivi(String azione, String risorsaTipo, String risorsaId, Esito esito, String motivo) {
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("timestamp", Instant.now().toString());
        evento.put("traceId", MDC.get(CorrelationIdFilter.MDC_KEY));
        evento.put("subject", currentSubject());
        evento.put("username", currentUsername());
        evento.put("azione", azione);
        evento.put("risorsaTipo", risorsaTipo);
        evento.put("risorsaId", risorsaId);
        evento.put("esito", esito.name());
        evento.put("ip", currentIp());
        if (motivo != null) {
            evento.put("motivo", motivo);
        }

        try {
            AUDIT.info(objectMapper.writeValueAsString(evento));
        } catch (JsonProcessingException e) {
            AUDIT.info("{\"azione\":\"" + azione + "\",\"errore\":\"serializzazione evento di audit fallita\"}");
        }
    }

    private String currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return SISTEMA;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        }
        return null;
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
