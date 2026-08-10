package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneRequest;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneResponse;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recensioni")
@Tag(name = "Gestione Recensioni", description = "Endpoint per le recensioni degli utenti")
@SecurityRequirement(name = "bearerAuth")
public class RecensioneController {

    @Autowired
    private RecensioneService service;

    @Autowired
    private AuditLogger auditLogger;

    @GetMapping("/{id}")
    @Operation(
            summary = "Restituisce una recensione tramite il suo ID",
            description = "Accessibile da qualsiasi utente autenticato. Restituisce 404 se non trovata."
    )
    public ResponseEntity<RecensioneResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/itinerario/{itinerarioId}")
    @Operation(
            summary = "Restituisce tutte le recensioni di un itinerario",
            description = "Dato l'ID di un itinerario, ritorna la lista di tutte le recensioni collegate. " +
                    "Accessibile da qualsiasi utente autenticato. Restituisce lista vuota se non ce ne sono."
    )
    public ResponseEntity<Page<RecensioneResponse>> leggiRecensioniItinerario(
            @PathVariable Long itinerarioId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getRecensioniDaItinerarioId(itinerarioId, pageable));
    }

    @GetMapping("/itinerario/{itinerarioId}/media")
    @Operation(
            summary = "Restituisce la media dei voti di un itinerario",
            description = "Calcola la media aritmetica dei voti. Accessibile da qualsiasi utente autenticato. " +
                    "Restituisce 0.0 se non ci sono recensioni."
    )
    public ResponseEntity<Double> getMediaVoti(@PathVariable Long itinerarioId) {
        return ResponseEntity.ok(service.getMediaVoti(itinerarioId));
    }

    @PostMapping
    @Operation(
            summary = "Aggiunge una nuova recensione",
            description = "Accessibile da qualsiasi utente autenticato. Riceve il DTO con itinerarioId (o prenotazioneId), " +
                    "voto e commento. Se viene passata la prenotazione, verifica che appartenga all'utente " +
                    "e che non sia già stata recensita."
    )
    public ResponseEntity<?> addNewRecensione(@Valid @RequestBody RecensioneRequest dto) {
        Long id = service.addNewRecensione(dto);
        auditLogger.success("RECENSIONE_CREATA", "Recensione", String.valueOf(id));
        return ResponseEntity.status(HttpStatus.CREATED).body("Recensione aggiunta con successo!");
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Rimuove una recensione",
            description = "Accessibile solo dall'autore della recensione. Restituisce 403 se si tenta " +
                    "di eliminare la recensione di un altro utente."
    )
    public ResponseEntity<?> removeRecensione(@PathVariable Long id) {
        service.deleteRecensione(id);
        auditLogger.success("RECENSIONE_ELIMINATA", "Recensione", String.valueOf(id));
        return ResponseEntity.ok().build();
    }
}