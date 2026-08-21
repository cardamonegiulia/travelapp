package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.mapper.ItinerarioMapper;
import com.unical.travelapp.backend.catalog.service.ItinerarioService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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
        Optional<Itinerario> itinerario = itinerarioService.getItinerarioById(id);
        return itinerario.map(it -> ResponseEntity.ok(itinerarioMapper.toDTO(it)))
                .orElseGet(() -> ResponseEntity.notFound().build());
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
}