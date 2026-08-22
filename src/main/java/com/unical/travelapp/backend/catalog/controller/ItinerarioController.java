package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import com.unical.travelapp.backend.catalog.mapper.ItinerarioMapper;
import com.unical.travelapp.backend.catalog.service.ItinerarioService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/itinerari")
@Tag(name = "Itinerari", description = "Gestione degli itinerari di viaggio")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(
            summary = "Restituisce tutti gli itinerari paginati",
            description = "Accessibile da qualsiasi utente autenticato. Default 20 itinerari per pagina."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista itinerari restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Page<ItinerarioDTO>> getAllItinerari(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(itinerarioService.getAllItinerari(pageable)
                .map(itinerarioMapper::toDTO));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Restituisce un itinerario per id",
            description = "Accessibile da qualsiasi utente autenticato. Restituisce 404 se non trovato."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itinerario trovato"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Itinerario non trovato")
    })
    public ResponseEntity<ItinerarioDTO> getItinerarioById(@PathVariable Long id) {
        Itinerario itinerario = itinerarioService.getItinerarioById(id)
                .orElseThrow(() -> new ItinerarioNonTrovatoException("Itinerario non trovato: " + id));
        return ResponseEntity.ok(itinerarioMapper.toDTO(itinerario));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(
            summary = "Crea un nuovo itinerario",
            description = "Accessibile solo da ORGANIZZATORE o ADMIN. L'organizzatore viene impostato automaticamente dal server tramite il token JWT. Lo stato iniziale è sempre BOZZA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itinerario creato con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ORGANIZZATORE o ADMIN")
    })
    public ResponseEntity<ItinerarioDTO> createItinerario(
            @Valid @RequestBody ItinerarioRequestDTO itinerarioRequest) {
        Itinerario entity = itinerarioMapper.fromRequest(itinerarioRequest);
        entity.setOrganizzatore(utenteService.getUtenteSessione());
        entity.setStato("BOZZA");
        Itinerario salvato = itinerarioService.saveItinerario(entity);
        auditLogger.success("ITINERARIO_CREATO", "Itinerario", String.valueOf(salvato.getId()));
        return ResponseEntity.ok(itinerarioMapper.toDTO(salvato));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(
            summary = "Aggiorna un itinerario esistente",
            description = "Accessibile da ORGANIZZATORE (solo i propri itinerari) o ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itinerario aggiornato con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ORGANIZZATORE (propri) o ADMIN"),
            @ApiResponse(responseCode = "404", description = "Itinerario non trovato")
    })
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
    @Operation(
            summary = "Elimina un itinerario per id",
            description = "Accessibile da ORGANIZZATORE (solo i propri itinerari) o ADMIN. L'eliminazione è permanente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Itinerario eliminato con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo ORGANIZZATORE (propri) o ADMIN"),
            @ApiResponse(responseCode = "404", description = "Itinerario non trovato")
    })
    public ResponseEntity<Void> deleteItinerario(@PathVariable Long id) {
        itinerarioService.deleteItinerario(id, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("ITINERARIO_ELIMINATO", "Itinerario", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/immagini", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(
            summary = "Aggiunge un'immagine a un itinerario",
            description = "Accessibile da ORGANIZZATORE (solo i propri itinerari) o ADMIN. La prima immagine caricata diventa automaticamente la copertina."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Immagine aggiunta con successo"),
            @ApiResponse(responseCode = "400", description = "File non valido"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Itinerario non trovato")
    })
    public ResponseEntity<ImmagineResponse> aggiungiImmagine(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        ImmagineResponse immagine = itinerarioService.aggiungiImmagine(
                id, file, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("IMMAGINE_AGGIUNTA", "Itinerario", String.valueOf(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(immagine);
    }

    @GetMapping("/{id}/immagini")
    @Operation(
            summary = "Restituisce le immagini di un itinerario",
            description = "Accessibile da qualsiasi utente autenticato. La prima immagine della lista è la copertina."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista immagini restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Itinerario non trovato")
    })
    public ResponseEntity<List<ImmagineResponse>> getImmagini(@PathVariable Long id) {
        return ResponseEntity.ok(itinerarioService.getImmagini(id));
    }

    @DeleteMapping("/{id}/immagini/{immagineId}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(
            summary = "Rimuove un'immagine da un itinerario",
            description = "Accessibile da ORGANIZZATORE (solo i propri itinerari) o ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Immagine rimossa con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Itinerario o immagine non trovati")
    })
    public ResponseEntity<Void> rimuoviImmagine(
            @PathVariable Long id,
            @PathVariable Long immagineId) {
        itinerarioService.rimuoviImmagine(
                id, immagineId, utenteService.getUtenteSessione(), utenteService.isAdmin());
        auditLogger.success("IMMAGINE_RIMOSSA", "Itinerario", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}