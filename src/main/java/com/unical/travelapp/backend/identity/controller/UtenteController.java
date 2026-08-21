package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.identity.dto.CambioPasswordRequest;
import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.dto.UtenteUpdateDto;
import com.unical.travelapp.backend.identity.service.UtenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    private final UtenteService utenteService;
    private final AuditLogger auditLogger;

    public UtenteController(UtenteService utenteService, AuditLogger auditLogger) {
        this.utenteService = utenteService;
        this.auditLogger = auditLogger;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea il record locale di un utente già esistente su Keycloak (uso amministrativo). "
            + "La registrazione self-service passa da POST /api/auth/registrazione")
    public ResponseEntity<UtenteResponseDto> creaUtente(@Valid @RequestBody UtenteDto utenteDto) {
        UtenteResponseDto creato = utenteService.salvaUtenteDatoDTO(utenteDto);
        auditLogger.success("UTENTE_CREATO", "Utente", String.valueOf(creato.getId()));
        return ResponseEntity.status(201).body(creato);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UtenteResponseDto>> getTuttiGliUtenti(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(utenteService.ottieniTutti(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    public ResponseEntity<UtenteResponseDto> getUtentePerId(@PathVariable Long id) {
        return ResponseEntity.ok(utenteService.ottieniPerId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    @Operation(summary = "Aggiorna i dati di un utente")
    public ResponseEntity<UtenteResponseDto> aggiornaUtente(
            @PathVariable Long id,
            @Valid @RequestBody UtenteUpdateDto utenteUpdateDto) {
        UtenteResponseDto aggiornato = utenteService.aggiornaUtente(id, utenteUpdateDto);
        auditLogger.success("UTENTE_MODIFICATO", "Utente", String.valueOf(id));
        return ResponseEntity.ok(aggiornato);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    @Operation(summary = "Elimina un utente per id")
    public ResponseEntity<Void> eliminaUtente(@PathVariable Long id) {
        utenteService.eliminaUtente(id);
        auditLogger.success("UTENTE_ELIMINATO", "Utente", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me")
    @Operation(summary = "Sincronizza l'utente loggato con il database locale")
    public ResponseEntity<UtenteResponseDto> sincronizzaUtente(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(utenteService.sincronizzaUtente(jwt));
    }

    @PostMapping("/me/password")
    @Operation(
            summary = "Cambia la password dell'utente autenticato",
            description = "Richiede un'autenticazione recente: il token deve portare un claim "
                    + "auth_time non piu' vecchio di app.security.max-auth-age-seconds. In caso "
                    + "contrario risponde 401 con WWW-Authenticate: il client deve rifare il "
                    + "login con max_age, non rinnovare il token col refresh. Al termine tutte "
                    + "le sessioni dell'utente vengono chiuse: serve un nuovo login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password cambiata; sessioni terminate"),
            @ApiResponse(responseCode = "400", description = "Password non conforme ai requisiti", content = @Content),
            @ApiResponse(responseCode = "401", description = "Autenticazione assente o troppo vecchia", content = @Content),
            @ApiResponse(responseCode = "503", description = "Keycloak non disponibile", content = @Content)
    })
    public ResponseEntity<Void> cambiaPassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CambioPasswordRequest richiesta) {
        Long id = utenteService.cambiaPassword(jwt, richiesta.getNuovaPassword());
        auditLogger.success("PASSWORD_CAMBIATA", "Utente", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}
