package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaRequestDTO;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.catalog.mapper.SingolaAttivitaMapper;
import com.unical.travelapp.backend.catalog.service.SingolaAttivitaService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.identity.service.UtenteService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.unical.travelapp.backend.catalog.exception.SingolaAttivitaNonTrovataException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attivita")
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
    public ResponseEntity<Page<SingolaAttivitaDTO>> getAllAttivita(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(attivitaService.getAllAttivita(pageable).map(attivitaMapper::toDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SingolaAttivitaDTO> getAttivitaById(@PathVariable Long id) {
        SingolaAttivita attivita = attivitaService.getAttivitaById(id)
                .orElseThrow(() -> new SingolaAttivitaNonTrovataException("Attività non trovata: " + id));
        return ResponseEntity.ok(attivitaMapper.toDTO(attivita));
    }

    // --- Endpoint per recuperare le sessioni (richiesto per il booking) ---
    @GetMapping("/{id}/sessioni")
    public ResponseEntity<List<SessioneSingolaAttivita>> getSessioniByAttivita(@PathVariable Long id) {
        return ResponseEntity.ok(attivitaService.getSessioniByAttivitaId(id));
    }

    @PostMapping("/con-sessioni")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<SingolaAttivitaDTO> createAttivitaConSessioni(
            @Valid @RequestBody SingolaAttivitaRequestDTO attivitaRequest,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inizio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fine,
            @RequestParam @NotEmpty(message = "Indicare almeno un giorno della settimana")
            @Size(max = 7, message = "I giorni della settimana sono al massimo 7")
            List<@Min(value = 1, message = "Giorno della settimana non valido")
            @Max(value = 7, message = "Giorno della settimana non valido") Integer> giorni) {

        SingolaAttivita entity = attivitaMapper.fromRequest(attivitaRequest);
        entity.setOrganizzatore(utenteService.getUtenteSessione());
        SingolaAttivita salvata = attivitaService.saveAttivitaConSessioni(entity, inizio, fine, giorni);
        auditLogger.success("ATTIVITA_CREATA", "SingolaAttivita", String.valueOf(salvata.getId()));
        return ResponseEntity.ok(attivitaMapper.toDTO(salvata));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
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
    public ResponseEntity<Void> deleteAttivita(@PathVariable Long id) {
        attivitaService.deleteAttivita(id, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("ATTIVITA_ELIMINATA", "SingolaAttivita", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}