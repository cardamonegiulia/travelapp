package com.unical.travelapp.backend.identity.controller;

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

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    private final UtenteService utenteService;

    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea un utente (uso amministrativo). La registrazione self-service passa da /api/utenti/me")
    public ResponseEntity<UtenteResponseDto> creaUtente(@Valid @RequestBody UtenteDto utenteDto) {
        return ResponseEntity.status(201).body(utenteService.salvaUtenteDatoDTO(utenteDto));
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
        return ResponseEntity.ok(utenteService.aggiornaUtente(id, utenteUpdateDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utenteSecurity.isSelf(#id, authentication)")
    @Operation(summary = "Elimina un utente per id")
    public ResponseEntity<Void> eliminaUtente(@PathVariable Long id) {
        utenteService.eliminaUtente(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me")
    @Operation(summary = "Sincronizza l'utente loggato con il database locale")
    public ResponseEntity<UtenteResponseDto> sincronizzaUtente(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(utenteService.sincronizzaUtente(jwt));
    }
}
