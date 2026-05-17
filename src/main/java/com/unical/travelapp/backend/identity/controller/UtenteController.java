package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    private final UtenteService utenteService;

    // Iniezione del Service
    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }


    @PostMapping
    public ResponseEntity<Utente> creaUtente(@Valid @RequestBody UtenteDto utenteDto) {
        // Usiamo il metodo nuovo dello Chef (Service) che sa leggere il DTO
        Utente nuovoUtente = utenteService.salvaUtenteDatoDTO(utenteDto);
        return ResponseEntity.ok(nuovoUtente);
    }

    // Rotta per leggere tutti gli utenti (GET)
    @GetMapping
    public ResponseEntity<List<Utente>> getTuttiGliUtenti() {
        return ResponseEntity.ok(utenteService.ottieniTutti());
    }
}