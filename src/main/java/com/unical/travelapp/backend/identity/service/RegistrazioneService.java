package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.identity.dto.RegistrazioneRequest;
import com.unical.travelapp.backend.identity.dto.RuoloRegistrabile;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.entity.Tema;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.RuoloRealmNonConfiguratoException;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.NuovoUtenteKeycloak;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.RuoloRealm;
import com.unical.travelapp.backend.identity.mapper.UtenteMapper;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class RegistrazioneService {

    private static final Logger log = LoggerFactory.getLogger(RegistrazioneService.class);

    private static final Set<String> RUOLI_AUTO_ASSEGNABILI = Set.of("VIAGGIATORE", "ORGANIZZATORE");

    private final UtenteRepository utenteRepository;
    private final UtenteMapper utenteMapper;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AuditLogger auditLogger;

    public RegistrazioneService(UtenteRepository utenteRepository,
                                UtenteMapper utenteMapper,
                                KeycloakAdminClient keycloakAdminClient,
                                AuditLogger auditLogger) {
        this.utenteRepository = utenteRepository;
        this.utenteMapper = utenteMapper;
        this.keycloakAdminClient = keycloakAdminClient;
        this.auditLogger = auditLogger;
    }

    public UtenteResponseDto registra(RegistrazioneRequest richiesta) {
        String email = normalizzaEmail(richiesta.getEmail());
        RuoloRegistrabile ruoloScelto = richiesta.getRuolo();
        String nomeRuoloRealm = ruoloScelto.nomeRuoloRealm();

        if (!RUOLI_AUTO_ASSEGNABILI.contains(nomeRuoloRealm)) {
            auditLogger.failure("REGISTRAZIONE", "Utente", email, "ruolo non auto-assegnabile: " + nomeRuoloRealm);
            throw new IllegalArgumentException("Il ruolo richiesto non è assegnabile in fase di registrazione");
        }

        if (utenteRepository.existsByEmail(email)) {
            auditLogger.failure("REGISTRAZIONE", "Utente", email, "email già registrata");
            throw new UtenteGiaEsistenteException("Esiste già un utente registrato con questa email");
        }

        String token = keycloakAdminClient.ottieniTokenAmministrativo();

        RuoloRealm ruoloRealm = keycloakAdminClient.trovaRuoloRealm(token, nomeRuoloRealm)
                .orElseThrow(() -> {
                    log.error("Ruolo realm '{}' assente su Keycloak: crearlo in Realm roles prima di "
                            + "abilitare la registrazione", nomeRuoloRealm);
                    auditLogger.failure("REGISTRAZIONE", "Utente", email,
                            "ruolo realm non configurato: " + nomeRuoloRealm);
                    return new RuoloRealmNonConfiguratoException(nomeRuoloRealm);
                });

        String keycloakId = keycloakAdminClient.creaUtente(token, new NuovoUtenteKeycloak(
                email,
                email,
                richiesta.getNome().trim(),
                richiesta.getCognome().trim(),
                richiesta.getPassword()));

        try {
            keycloakAdminClient.assegnaRuoloRealm(token, keycloakId, ruoloRealm);

            Utente utente = new Utente();
            utente.setKeycloakId(keycloakId);
            utente.setNome(richiesta.getNome().trim());
            utente.setCognome(richiesta.getCognome().trim());
            utente.setEmail(email);
            utente.setRuolo(ruoloScelto.toRuolo());
            utente.setTema(Tema.CHIARO);

            Utente salvato = utenteRepository.save(utente);
            auditLogger.success("REGISTRAZIONE", "Utente", String.valueOf(salvato.getId()));
            return utenteMapper.toResponseDto(salvato);

        } catch (RuntimeException e) {
            auditLogger.failure("REGISTRAZIONE", "Utente", email, e.getClass().getSimpleName());
            keycloakAdminClient.eliminaUtenteSenzaPropagareErrori(token, keycloakId);
            throw e;
        }
    }

    private String normalizzaEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
