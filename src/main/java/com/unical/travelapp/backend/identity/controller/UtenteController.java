package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.dto.UtenteUpdateDto;
import com.unical.travelapp.backend.identity.service.UtenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            summary = "Crea il record locale di un utente già esistente su Keycloak",
            description = "Uso amministrativo. La registrazione self-service passa da POST /api/auth/registrazione. Solo ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utente creato con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ADMIN"),
            @ApiResponse(responseCode = "409", description = "Utente già esistente con stessa email o keycloakId")
    })
    public ResponseEntity<UtenteResponseDto> creaUtente(@Valid @RequestBody UtenteDto utenteDto) {
        UtenteResponseDto creato = utenteService.salvaUtenteDatoDTO(utenteDto);
        auditLogger.success("UTENTE_CREATO", "Utente", String.valueOf(creato.getId()));
        return ResponseEntity.status(201).body(creato);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Restituisce tutti gli utenti paginati",
            description = "Accessibile solo da ADMIN. Default 20 utenti per pagina."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista utenti restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ADMIN")
    })
    public ResponseEntity<Page<UtenteResponseDto>> getTuttiGliUtenti(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(utenteService.ottieniTutti(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    @Operation(
            summary = "Restituisce un utente per id",
            description = "Accessibile da ADMIN o dal proprietario dell'account. Restituisce 404 se la risorsa non appartiene all'utente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utente trovato"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public ResponseEntity<UtenteResponseDto> getUtentePerId(@PathVariable Long id) {
        return ResponseEntity.ok(utenteService.ottieniPerId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    @Operation(
            summary = "Aggiorna i dati di un utente",
            description = "Accessibile da ADMIN o dal proprietario dell'account. Permette di modificare nome, cognome, email e tema."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utente aggiornato con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato"),
            @ApiResponse(responseCode = "409", description = "Email già in uso da un altro utente")
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Utente eliminato con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utente sincronizzato con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "500", description = "Errore interno del server")
    })
    public ResponseEntity<UtenteResponseDto> sincronizzaUtente(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(utenteService.sincronizzaUtente(jwt));
    }
}