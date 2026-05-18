package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.dto.UtenteUpdateDto;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    private final UtenteService utenteService;

    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @PostMapping
    public ResponseEntity<UtenteResponseDto> creaUtente(@Valid @RequestBody UtenteDto utenteDto) {
        return ResponseEntity.status(201).body(utenteService.salvaUtenteDatoDTO(utenteDto));
    }

    @GetMapping
    public ResponseEntity<List<UtenteResponseDto>> getTuttiGliUtenti() {
        return ResponseEntity.ok(utenteService.ottieniTutti());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtenteResponseDto> getUtentePerId(@PathVariable Long id) {
        return ResponseEntity.ok(utenteService.ottieniPerId(id));
    }
    @PutMapping("/{id}")
    @Operation(summary = "Aggiorna i dati di un utente")
    public ResponseEntity<UtenteResponseDto> aggiornaUtente(
            @PathVariable Long id,
            @Valid @RequestBody UtenteUpdateDto utenteUpdateDto) {
        return ResponseEntity.ok(utenteService.aggiornaUtente(id, utenteUpdateDto));
    }
}