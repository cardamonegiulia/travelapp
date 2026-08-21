package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaRequestDTO;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.catalog.mapper.SingolaAttivitaMapper;
import com.unical.travelapp.backend.catalog.service.SingolaAttivitaService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.identity.service.UtenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/attivita")
@Tag(name = "Attività", description = "Gestione delle attività turistiche")
@SecurityRequirement(name = "bearerAuth")
public class SingolaAttivitaController {

    @Autowired
    private SingolaAttivitaService attivitaService;

    @Autowired
    private SingolaAttivitaMapper attivitaMapper;

    @Autowired
    private UtenteService utenteService;

    @Autowired
    private AuditLogger auditLogger;

    @GetMapping
    @Operation(
            summary = "Restituisce tutte le attività paginata",
            description = "Accessibile da qualsiasi utente autenticato. Default 20 attività per pagina."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista attività restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Page<SingolaAttivitaDTO>> getAllAttivita(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(attivitaService.getAllAttivita(pageable)
                .map(attivitaMapper::toDTO));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Restituisce un'attività per id",
            description = "Accessibile da qualsiasi utente autenticato. Restituisce 404 se non trovata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attività trovata"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Attività non trovata")
    })
    public ResponseEntity<SingolaAttivitaDTO> getAttivitaById(@PathVariable Long id) {
        Optional<SingolaAttivita> attivita = attivitaService.getAttivitaById(id);
        return attivita.map(att -> ResponseEntity.ok(attivitaMapper.toDTO(att)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/con-sessioni")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(
            summary = "Crea una nuova attività con sessioni ricorrenti",
            description = "Accessibile solo da ORGANIZZATORE o ADMIN. L'organizzatore viene impostato automaticamente " +
                    "dal server tramite il token JWT. Parametri: inizio e fine definiscono il periodo, " +
                    "giorni indica i giorni della settimana (1=Lunedì, 7=Domenica)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attività creata con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi — controlla date e giorni"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ORGANIZZATORE o ADMIN")
    })
    public ResponseEntity<SingolaAttivitaDTO> createAttivitaConSessioni(
            @Valid @RequestBody SingolaAttivitaRequestDTO attivitaRequest,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inizio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fine,
            @RequestParam List<Integer> giorni) {
        SingolaAttivita entity = attivitaMapper.fromRequest(attivitaRequest);
        entity.setOrganizzatore(utenteService.getUtenteSessione());
        SingolaAttivita salvata = attivitaService.saveAttivitaConSessioni(entity, inizio, fine, giorni);
        auditLogger.success("ATTIVITA_CREATA", "SingolaAttivita", String.valueOf(salvata.getId()));
        return ResponseEntity.ok(attivitaMapper.toDTO(salvata));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(
            summary = "Elimina un'attività per id",
            description = "Accessibile da ORGANIZZATORE (solo le proprie attività) o ADMIN. L'eliminazione è permanente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attività eliminata con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ORGANIZZATORE (proprie) o ADMIN"),
            @ApiResponse(responseCode = "404", description = "Attività non trovata")
    })
    public ResponseEntity<Void> deleteAttivita(@PathVariable Long id) {
        attivitaService.deleteAttivita(id, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("ATTIVITA_ELIMINATA", "SingolaAttivita", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}