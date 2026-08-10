package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.dto.UtenteUpdateDto;
import com.unical.travelapp.backend.identity.service.UtenteService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/utenti")
@Tag(name = "Utenti", description = "Gestione degli utenti")
@SecurityRequirement(name = "bearerAuth")
public class UtenteController {


    private final UtenteService utenteService;
    private final AuditLogger auditLogger;

    public UtenteController(UtenteService utenteService, AuditLogger auditLogger) {
        this.utenteService = utenteService;
        this.auditLogger = auditLogger;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Restituisce tutti gli utenti paginati",
            description = "Accessibile solo da ADMIN. Risultati paginati, massimo 100 per pagina."
    )
    public ResponseEntity<UtenteResponseDto> creaUtente(@Valid @RequestBody UtenteDto utenteDto) {
        UtenteResponseDto creato = utenteService.salvaUtenteDatoDTO(utenteDto);
        auditLogger.success("UTENTE_CREATO", "Utente", String.valueOf(creato.getId()));
        return ResponseEntity.status(201).body(creato);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Restituisce tutti gli utenti paginati",
            description = "Accessibile solo da ADMIN. Risultati paginati, massimo 100 per pagina."
    )
    public ResponseEntity<Page<UtenteResponseDto>> getTuttiGliUtenti(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(utenteService.ottieniTutti(pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Restituisce un utente per id",
            description = "Accessibile da ADMIN o dal proprietario dell'account. Restituisce 404 se la risorsa non appartiene all'utente."
    )
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    public ResponseEntity<UtenteResponseDto> getUtentePerId(@PathVariable Long id) {
        return ResponseEntity.ok(utenteService.ottieniPerId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    @Operation(
            summary = "Aggiorna i dati di un utente",
            description = "Accessibile da ADMIN o dal proprietario dell'account. Permette di modificare nome, cognome, email e tema."
    )
    public ResponseEntity<UtenteResponseDto> aggiornaUtente(
            @PathVariable Long id,
            @Valid @RequestBody UtenteUpdateDto utenteUpdateDto) {
        UtenteResponseDto aggiornato = utenteService.aggiornaUtente(id, utenteUpdateDto);
        auditLogger.success("UTENTE_MODIFICATO", "Utente", String.valueOf(id));
        return ResponseEntity.ok(aggiornato);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    @Operation(
            summary = "Elimina un utente per id",
            description = "Accessibile da ADMIN o dal proprietario dell'account. L'eliminazione è permanente."
    )
    public ResponseEntity<Void> eliminaUtente(@PathVariable Long id) {
        utenteService.eliminaUtente(id);
        auditLogger.success("UTENTE_ELIMINATO", "Utente", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me")
    @Operation(
            summary = "Sincronizza l'utente loggato con il database locale",
            description = "Da chiamare al primo accesso dopo il login con Keycloak. Crea il record locale se non esiste, altrimenti restituisce quello esistente."
    )
    public ResponseEntity<UtenteResponseDto> sincronizzaUtente(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(utenteService.sincronizzaUtente(jwt));
    }
}
