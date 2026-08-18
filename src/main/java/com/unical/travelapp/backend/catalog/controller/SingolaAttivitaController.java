package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaRequestDTO;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        Optional<SingolaAttivita> attivita = attivitaService.getAttivitaById(id);
        return attivita.map(att -> ResponseEntity.ok(attivitaMapper.toDTO(att)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * I parametri della query non erano validati, a differenza del corpo: l'endpoint genera
     * una sessione per ogni giorno dell'intervallo che ricade nei giorni scelti, quindi
     * {@code inizio}, {@code fine} e {@code giorni} decidono quanto lavoro fa il server. Il
     * tetto sull'ampiezza dell'intervallo sta nel service, dov'e' la regola di dominio; qui
     * restano i vincoli sulla forma dei valori.
     */
    @PostMapping("/con-sessioni")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<SingolaAttivitaDTO> createAttivitaConSessioni(
            @Valid @RequestBody SingolaAttivitaRequestDTO attivitaRequest,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inizio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fine,
            // allow-list sui giorni della settimana: 1 (lunedi') .. 7 (domenica), come
            // DayOfWeek.getValue(). Senza il tetto, una lista lunga a piacere veniva
            // interrogata a ogni giorno dell'intervallo
            @RequestParam @NotEmpty(message = "Indicare almeno un giorno della settimana")
            @Size(max = 7, message = "I giorni della settimana sono al massimo 7")
            List<@Min(value = 1, message = "Giorno della settimana non valido")
                 @Max(value = 7, message = "Giorno della settimana non valido") Integer> giorni) {

        SingolaAttivita entity = attivitaMapper.fromRequest(attivitaRequest);
        // l'organizzatore e' sempre l'utente autenticato, mai un id passato dal client
        entity.setOrganizzatore(utenteService.getUtenteSessione());
        SingolaAttivita salvata = attivitaService.saveAttivitaConSessioni(entity, inizio, fine, giorni);
        auditLogger.success("ATTIVITA_CREATA", "SingolaAttivita", String.valueOf(salvata.getId()));
        return ResponseEntity.ok(attivitaMapper.toDTO(salvata));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<Void> deleteAttivita(@PathVariable Long id) {
        attivitaService.deleteAttivita(id, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("ATTIVITA_ELIMINATA", "SingolaAttivita", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}