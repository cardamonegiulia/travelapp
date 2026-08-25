package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.identity.dto.CambioPasswordRequest;
import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.dto.UtenteUpdateDto;
import com.unical.travelapp.backend.identity.service.FotoProfiloService;
import com.unical.travelapp.backend.identity.service.UtenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/utenti")
@Tag(name = "Utenti", description = "Gestione degli utenti")
@SecurityRequirement(name = "bearerAuth")
public class UtenteController {

    private final UtenteService utenteService;
    private final FotoProfiloService fotoProfiloService;
    private final AuditLogger auditLogger;

    public UtenteController(UtenteService utenteService,
                            FotoProfiloService fotoProfiloService,
                            AuditLogger auditLogger) {
        this.utenteService = utenteService;
        this.fotoProfiloService = fotoProfiloService;
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
            @ApiResponse(responseCode = "400", description = "Dati non validi", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ADMIN", content = @Content),
            @ApiResponse(responseCode = "409", description = "Utente già esistente con stessa email o keycloakId", content = @Content)
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
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ADMIN", content = @Content)
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
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utente non trovato", content = @Content)
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
            @ApiResponse(responseCode = "400", description = "Dati non validi", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utente non trovato", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email già in uso da un altro utente", content = @Content)
    })
    public ResponseEntity<UtenteResponseDto> aggiornaUtente(
            @PathVariable Long id,
            @Valid @RequestBody UtenteUpdateDto utenteUpdateDto) {
        UtenteResponseDto aggiornato = utenteService.aggiornaUtente(id, utenteUpdateDto);
        auditLogger.success("UTENTE_MODIFICATO", "Utente", String.valueOf(id));
        return ResponseEntity.ok(aggiornato);
    }

    @PutMapping("/{id}/ruolo/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promuove un utente al ruolo di ADMIN (solo per amministratori)")
    public ResponseEntity<UtenteResponseDto> promuoviAdAdmin(@PathVariable Long id) {
        UtenteResponseDto aggiornato = utenteService.promuoviAdAdmin(id);
        auditLogger.success("UTENTE_PROMOSSO_ADMIN", "Utente", String.valueOf(id));
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
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utente non trovato", content = @Content)
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
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "500", description = "Errore interno del server", content = @Content)
    })
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
    public ResponseEntity<Void> cambiaPassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CambioPasswordRequest richiesta) {
        Long id = utenteService.cambiaPassword(jwt, richiesta.getNuovaPassword());
        auditLogger.success("PASSWORD_CAMBIATA", "Utente", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }

    // --- Foto profilo ---------------------------------------------------------------------

    @PutMapping(value = "/me/foto-profilo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Imposta (o sostituisce) la foto profilo dell'utente autenticato"
    )
    public ResponseEntity<UtenteResponseDto> impostaFotoProfilo(@RequestParam("file") MultipartFile file) {
        UtenteResponseDto aggiornato = fotoProfiloService.imposta(file);
        auditLogger.success("FOTO_PROFILO_IMPOSTATA", "Utente", String.valueOf(aggiornato.getId()));
        return ResponseEntity.ok(aggiornato);
    }

    @DeleteMapping("/me/foto-profilo")
    @Operation(summary = "Rimuove la foto profilo dell'utente autenticato")
    public ResponseEntity<Void> rimuoviFotoProfilo() {
        fotoProfiloService.rimuovi();
        auditLogger.success("FOTO_PROFILO_RIMOSSA", "Utente", "me");
        return ResponseEntity.noContent().build();
    }
}