package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utenti")
@Tag(name = "Utenti", description = "API per la gestione dell'identità")
public class UtenteController {

    private final UtenteService utenteService;

    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @PostMapping
    @Operation(summary = "Crea un nuovo utente")
    public Utente creaUtente(@RequestBody Utente utente) {
        return utenteService.creaUtente(utente);
    }

    @GetMapping
    @Operation(summary = "Recupera tutti gli utenti registrati")
    public List<Utente> getTuttiGliUtenti() {
        return utenteService.getAllUtenti();
    }
}