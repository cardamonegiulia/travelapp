package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.experience.models.DTO.PreferitoDTO;
import com.unical.travelapp.backend.experience.models.DTO.PreferitoItinerarioRequest;
import com.unical.travelapp.backend.experience.services.PreferitoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferiti")
@Tag(name = "Gestione Preferiti", description = "Endpoint degli itinerari preferiti degli utenti")
@SecurityRequirement(name = "bearerAuth")
public class PreferitoController {

    @Autowired
    private PreferitoService service;

    @GetMapping
    @Operation(
            summary = "Restituisce la lista dei preferiti dell'utente",
            description = "Accessibile da qualsiasi utente autenticato. Restituisce sempre e solo i preferiti dell'utente loggato. Restituisce 404 se non ha preferiti."
    )
    public ResponseEntity<?> getPreferiti() {
        PreferitoDTO dto = service.getPreferiti();
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @PostMapping
    @Operation(
            summary = "Aggiunge un itinerario alla lista dei preferiti",
            description = "Accessibile da qualsiasi utente autenticato. Riceve l'id di un itinerario e lo aggiunge alla lista dei preferiti dell'utente loggato."
    )
    public ResponseEntity<?> addItinerarioNeiPreferiti(
            @Valid @RequestBody PreferitoItinerarioRequest request) {
        service.addPreferito(request.getItinerarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @DeleteMapping
    @Operation(
            summary = "Rimuove un itinerario dalla lista dei preferiti",
            description = "Accessibile da qualsiasi utente autenticato. Riceve l'id di un itinerario e lo rimuove dalla lista dei preferiti dell'utente loggato."
    )
    public ResponseEntity<?> removeItinerarioDaPreferiti(
            @Valid @RequestBody PreferitoItinerarioRequest request) {
        service.removePreferito(request.getItinerarioId());
        return ResponseEntity.ok(null);
    }
}