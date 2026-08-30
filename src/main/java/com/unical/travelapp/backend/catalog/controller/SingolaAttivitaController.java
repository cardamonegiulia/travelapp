package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.SessioneSingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaRequestDTO;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.catalog.exception.SingolaAttivitaNonTrovataException;
import com.unical.travelapp.backend.catalog.mapper.SingolaAttivitaMapper;
import com.unical.travelapp.backend.catalog.service.SingolaAttivitaService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.identity.service.UtenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

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
    @Operation(summary = "Restituisce tutte le attività paginato")
    public ResponseEntity<Page<SingolaAttivitaDTO>> getAllAttivita(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(attivitaService.getAllAttivita(pageable).map(attivitaMapper::toDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Restituisce un'attività per id")
    public ResponseEntity<SingolaAttivitaDTO> getAttivitaById(@PathVariable Long id) {
        SingolaAttivita attivita = attivitaService.getAttivitaById(id)
                .orElseThrow(() -> new SingolaAttivitaNonTrovataException("Attività non trovata: " + id));
        return ResponseEntity.ok(attivitaMapper.toDTO(attivita));
    }

    @GetMapping("/{id}/sessioni")
    @Operation(summary = "Restituisce le sessioni di un'attività")
    public ResponseEntity<List<SessioneSingolaAttivitaDTO>> getSessioniByAttivita(@PathVariable Long id) {
        return ResponseEntity.ok(attivitaMapper.toSessioneDTO(
                attivitaService.getSessioniByAttivitaId(id)));
    }

    @PostMapping("/con-sessioni")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(summary = "Crea una nuova attività con sessioni ricorrenti")
    public ResponseEntity<SingolaAttivitaDTO> createAttivitaConSessioni(
            @Valid @RequestBody SingolaAttivitaRequestDTO attivitaRequest,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inizio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fine,
            @RequestParam @NotEmpty(message = "Indicare almeno un giorno della settimana")
            @Size(max = 7, message = "I giorni della settimana sono al massimo 7")
            List<@Min(value = 1, message = "Giorno non valido") @Max(value = 7, message = "Giorno non valido") Integer> giorni) {

        SingolaAttivita entity = attivitaMapper.fromRequest(attivitaRequest);
        entity.setOrganizzatore(utenteService.getUtenteSessione());
        SingolaAttivita salvata = attivitaService.saveAttivitaConSessioni(entity, inizio, fine, giorni);
        auditLogger.success("ATTIVITA_CREATA", "SingolaAttivita", String.valueOf(salvata.getId()));
        return ResponseEntity.ok(attivitaMapper.toDTO(salvata));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(summary = "Aggiorna un'attività esistente")
    public ResponseEntity<SingolaAttivitaDTO> updateAttivita(
            @PathVariable Long id,
            @Valid @RequestBody SingolaAttivitaRequestDTO attivitaRequest) {
        SingolaAttivita entity = attivitaMapper.fromRequest(attivitaRequest);
        SingolaAttivita aggiornata = attivitaService.updateAttivita(
                id, entity, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("ATTIVITA_MODIFICATA", "SingolaAttivita", String.valueOf(id));
        return ResponseEntity.ok(attivitaMapper.toDTO(aggiornata));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(summary = "Elimina un'attività per id")
    public ResponseEntity<Void> deleteAttivita(@PathVariable Long id) {
        attivitaService.deleteAttivita(id, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("ATTIVITA_ELIMINATA", "SingolaAttivita", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/immagini", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(summary = "Aggiunge un'immagine a un'attività")
    public ResponseEntity<ImmagineResponse> aggiungiImmagine(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        ImmagineResponse immagine = attivitaService.aggiungiImmagine(
                id, file, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("IMMAGINE_AGGIUNTA", "SingolaAttivita", String.valueOf(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(immagine);
    }

    @GetMapping("/{id}/immagini")
    @Operation(summary = "Restituisce le immagini di un'attività")
    public ResponseEntity<List<ImmagineResponse>> getImmagini(@PathVariable Long id) {
        return ResponseEntity.ok(attivitaService.getImmagini(id));
    }

    @DeleteMapping("/{id}/immagini/{immagineId}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(summary = "Rimuove un'immagine da un'attività")
    public ResponseEntity<Void> rimuoviImmagine(
            @PathVariable Long id,
            @PathVariable Long immagineId) {
        attivitaService.rimuoviImmagine(
                id, immagineId, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("IMMAGINE_RIMOSSA", "SingolaAttivita", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}