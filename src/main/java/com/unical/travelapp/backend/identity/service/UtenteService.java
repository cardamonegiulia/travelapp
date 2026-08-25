package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.dto.UtenteUpdateDto;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Tema;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.exception.RiautenticazioneRichiestaException;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.ProfiloKeycloak;
import com.unical.travelapp.backend.identity.mapper.UtenteMapper;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class UtenteService {

    private static final Logger log = LoggerFactory.getLogger(UtenteService.class);

    private final UtenteRepository utenteRepository;
    private final UtenteMapper utenteMapper;
    private final KeycloakAdminClient keycloakAdminClient;
    private final long etaMassimaAutenticazioneSecondi;

    public UtenteService(UtenteRepository utenteRepository,
                         UtenteMapper utenteMapper,
                         KeycloakAdminClient keycloakAdminClient,
                         @Value("${app.security.max-auth-age-seconds:300}") long etaMassimaAutenticazioneSecondi) {
        this.utenteRepository = utenteRepository;
        this.utenteMapper = utenteMapper;
        this.keycloakAdminClient = keycloakAdminClient;
        this.etaMassimaAutenticazioneSecondi = etaMassimaAutenticazioneSecondi;
    }

    public UtenteResponseDto salvaUtenteDatoDTO(UtenteDto dto) {
        if (utenteRepository.existsByEmail(dto.getEmail())) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con email: " + dto.getEmail()
            );
        }
        if (utenteRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con keycloakId: " + dto.getKeycloakId()
            );
        }
        return utenteMapper.toResponseDto(
                utenteRepository.save(utenteMapper.toEntity(dto))
        );
    }

    public Page<UtenteResponseDto> ottieniTutti(Pageable pageable) {
        return utenteRepository.findAll(pageable)
                .map(utenteMapper::toResponseDto);
    }

    public UtenteResponseDto ottieniPerId(Long id) {
        return utenteMapper.toResponseDto(
                utenteRepository.findById(id)
                        .orElseThrow(() -> new UtenteNonTrovatoException(
                                "Utente con id " + id + " non trovato"
                        ))
        );
    }

    public UtenteResponseDto ottieniPerKeycloakId(String keycloakId) {
        return utenteMapper.toResponseDto(
                utenteRepository.findByKeycloakId(keycloakId)
                        .orElseThrow(() -> new UtenteNonTrovatoException(
                                "Utente con keycloakId " + keycloakId + " non trovato"
                        ))
        );
    }

    public Utente salvaUtente(Utente utente) {
        return utenteRepository.save(utente);
    }

    public UtenteResponseDto promuoviAdAdmin(Long id) {
        Utente utente = utenteRepository.findById(id)
                .orElseThrow(() -> new UtenteNonTrovatoException("Utente con id " + id + " non trovato"));

        utente.setRuolo(Ruolo.ADMIN);
        Utente salvato = utenteRepository.save(utente);
        log.info("Utente {} promosso a ruolo ADMIN", id);
        return utenteMapper.toResponseDto(salvato);
    }

    public UtenteResponseDto aggiornaUtente(Long id, UtenteUpdateDto dto) {
        Utente utente = utenteRepository.findById(id)
                .orElseThrow(() -> new UtenteNonTrovatoException(
                        "Utente con id " + id + " non trovato"
                ));

        String emailRichiesta = dto.getEmail() == null ? null : normalizzaEmail(dto.getEmail());

        if (emailRichiesta != null &&
                !utente.getEmail().equals(emailRichiesta) &&
                utenteRepository.existsByEmail(emailRichiesta)) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con email: " + emailRichiesta
            );
        }

        ProfiloKeycloak precedente = profiloDi(utente);

        utenteMapper.updateEntity(utente, dto);
        if (emailRichiesta != null) {
            utente.setEmail(emailRichiesta);
        }

        ProfiloKeycloak aggiornato = profiloDi(utente);
        if (aggiornato.equals(precedente)) {
            return utenteMapper.toResponseDto(utenteRepository.save(utente));
        }

        boolean emailCambiata = !Objects.equals(aggiornato.email(), precedente.email());

        String token = keycloakAdminClient.ottieniTokenAmministrativo();
        keycloakAdminClient.aggiornaProfilo(token, utente.getKeycloakId(), aggiornato, emailCambiata);
        try {
            return utenteMapper.toResponseDto(utenteRepository.save(utente));
        } catch (RuntimeException e) {
            keycloakAdminClient.aggiornaProfiloSenzaPropagareErrori(token, utente.getKeycloakId(), precedente);
            throw e;
        }
    }

    private ProfiloKeycloak profiloDi(Utente utente) {
        return new ProfiloKeycloak(utente.getEmail(), utente.getNome(), utente.getCognome());
    }

    public void eliminaUtente(Long id) {
        Utente utente = utenteRepository.findById(id)
                .orElseThrow(() -> new UtenteNonTrovatoException(
                        "Utente con id " + id + " non trovato"
                ));

        keycloakAdminClient.eliminaUtente(
                keycloakAdminClient.ottieniTokenAmministrativo(), utente.getKeycloakId());

        utenteRepository.deleteById(id);
        log.info("Utente {} cancellato in locale e su Keycloak", id);
    }

    public Long cambiaPassword(Jwt jwt, String nuovaPassword) {
        Instant autenticatoIl = jwt.getClaimAsInstant("auth_time");
        if (autenticatoIl == null
                || autenticatoIl.isBefore(Instant.now().minusSeconds(etaMassimaAutenticazioneSecondi))) {
            throw new RiautenticazioneRichiestaException(etaMassimaAutenticazioneSecondi);
        }

        Utente utente = ottieniUtenteDaToken(jwt);

        String token = keycloakAdminClient.ottieniTokenAmministrativo();
        keycloakAdminClient.impostaPassword(token, utente.getKeycloakId(), nuovaPassword);
        keycloakAdminClient.terminaSessioniSenzaPropagareErrori(token, utente.getKeycloakId());

        log.info("Password cambiata per l'utente {}; sessioni terminate", utente.getId());
        return utente.getId();
    }

    public Utente ottieniUtenteDaToken(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return utenteRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UtenteNonTrovatoException("Utente loggato non trovato nel database locale"));
    }

    public Utente getUtenteSessione(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return ottieniUtenteDaToken(jwt);
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public Long ottieniIdDaToken(Jwt jwt) {
        return ottieniUtenteDaToken(jwt).getId();
    }

    public UtenteResponseDto sincronizzaUtente(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        return utenteRepository.findByKeycloakId(keycloakId)
                .map(this::riallineaRuolo)
                .map(utenteMapper::toResponseDto)
                .orElseGet(() -> utenteMapper.toResponseDto(creaDaToken(keycloakId, jwt)));
    }

    private Utente riallineaRuolo(Utente utente) {
        return ruoloDalTokenCorrente()
                .filter(ruoloDelToken -> ruoloDelToken != utente.getRuolo())
                .map(ruoloDelToken -> {
                    log.info("Ruolo locale dell'utente {} riallineato da {} a {} (fonte: token)",
                            utente.getId(), utente.getRuolo(), ruoloDelToken);
                    utente.setRuolo(ruoloDelToken);
                    return utenteRepository.save(utente);
                })
                .orElse(utente);
    }

    private Utente creaDaToken(String keycloakId, Jwt jwt) {
        String email = normalizzaEmail(jwt.getClaimAsString("email"));

        if (email.isEmpty()) {
            throw new IllegalArgumentException(
                    "Il token non contiene l'email necessaria a creare l'account locale");
        }
        if (utenteRepository.existsByEmail(email)) {
            throw new UtenteGiaEsistenteException("Esiste già un utente registrato con questa email");
        }

        Utente nuovo = new Utente();
        nuovo.setKeycloakId(keycloakId);
        nuovo.setNome(claimOppureVuoto(jwt, "given_name"));
        nuovo.setCognome(claimOppureVuoto(jwt, "family_name"));
        nuovo.setEmail(email);
        nuovo.setRuolo(ruoloDalTokenCorrente().orElse(Ruolo.VIAGGIATORE));
        nuovo.setTema(Tema.CHIARO);
        return utenteRepository.save(nuovo);
    }

    private Optional<Ruolo> ruoloDalTokenCorrente() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return Stream.of(Ruolo.ADMIN, Ruolo.ORGANIZZATORE, Ruolo.VIAGGIATORE)
                .filter(ruolo -> authorities.stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + ruolo.name())))
                .findFirst();
    }

    private String normalizzaEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String claimOppureVuoto(Jwt jwt, String claim) {
        String valore = jwt.getClaimAsString(claim);
        return valore != null ? valore : "";
    }
}