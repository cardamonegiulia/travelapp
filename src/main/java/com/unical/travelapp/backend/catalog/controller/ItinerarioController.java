package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.mapper.ItinerarioMapper;
import com.unical.travelapp.backend.catalog.service.ItinerarioService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.identity.service.UtenteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/itinerari")
public class ItinerarioController {

    @Autowired
    private ItinerarioService itinerarioService;
    @Autowired
    private ItinerarioMapper itinerarioMapper;

    @Autowired
    private UtenteService utenteService;

    @Autowired
    private AuditLogger auditLogger;

    @GetMapping
    public ResponseEntity<Page<ItinerarioDTO>> getAllItinerari(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(itinerarioService.getAllItinerari(pageable).map(itinerarioMapper::toDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItinerarioDTO> getItinerarioById(@PathVariable Long id) {
        Itinerario itinerario = itinerarioService.getItinerarioById(id)
                .orElseThrow(() -> new ItinerarioNonTrovatoException("Itinerario non trovato: " + id));
        return ResponseEntity.ok(itinerarioMapper.toDTO(itinerario));
    }

    // --- Endpoint per recuperare le disponibilità (richiesto per il booking) ---
    @GetMapping("/{id}/disponibilita")
    public ResponseEntity<List<DisponibilitaItinerario>> getDisponibilitaByItinerario(@PathVariable Long id) {
        return ResponseEntity.ok(itinerarioService.getDisponibilitaByItinerarioId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<ItinerarioDTO> createItinerario(@Valid @RequestBody ItinerarioRequestDTO itinerarioRequest) {
        Itinerario entity = itinerarioMapper.fromRequest(itinerarioRequest);
        entity.setOrganizzatore(utenteService.getUtenteSessione());
        entity.setStato("BOZZA");
        Itinerario salvato = itinerarioService.saveItinerario(entity);
        auditLogger.success("ITINERARIO_CREATO", "Itinerario", String.valueOf(salvato.getId()));
        return ResponseEntity.ok(itinerarioMapper.toDTO(salvato));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<ItinerarioDTO> updateItinerario(
            @PathVariable Long id,
            @Valid @RequestBody ItinerarioRequestDTO itinerarioRequest) {
        Itinerario entity = itinerarioMapper.fromRequest(itinerarioRequest);
        Itinerario aggiornato = itinerarioService.updateItinerario(
                id, entity, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("ITINERARIO_MODIFICATO", "Itinerario", String.valueOf(id));
        return ResponseEntity.ok(itinerarioMapper.toDTO(aggiornato));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<Void> deleteItinerario(@PathVariable Long id) {
        itinerarioService.deleteItinerario(id, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("ITINERARIO_ELIMINATO", "Itinerario", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }

    // --- Immagini dell'itinerario ---------------------------------------------------------

    @PostMapping(value = "/{id}/immagini", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<ImmagineResponse> aggiungiImmagine(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {

        ImmagineResponse immagine = itinerarioService.aggiungiImmagine(
                id, file, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("IMMAGINE_AGGIUNTA", "Itinerario", String.valueOf(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(immagine);
    }

    @GetMapping("/{id}/immagini")
    public ResponseEntity<List<ImmagineResponse>> getImmagini(@PathVariable Long id) {
        return ResponseEntity.ok(itinerarioService.getImmagini(id));
    }

    @DeleteMapping("/{id}/immagini/{immagineId}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<Void> rimuoviImmagine(@PathVariable Long id, @PathVariable Long immagineId) {
        itinerarioService.rimuoviImmagine(
                id, immagineId, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("IMMAGINE_RIMOSSA", "Itinerario", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}