package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
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

    // Rotta per creare un nuovo utente (POST)
    @PostMapping
    public ResponseEntity<Utente> creaUtente(@RequestBody Utente utente) {
        Utente nuovoUtente = utenteService.salvaUtente(utente);
        return ResponseEntity.ok(nuovoUtente);
    }

    // Rotta per leggere tutti gli utenti (GET)
    @GetMapping
    public ResponseEntity<List<Utente>> getTuttiGliUtenti() {
        return ResponseEntity.ok(utenteService.ottieniTutti());
    }
}