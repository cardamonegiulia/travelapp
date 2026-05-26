package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.experience.models.DTO.PreferitoDTO;
import com.unical.travelapp.backend.experience.services.PreferitoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferiti")
@Tag(name = "Gestione Preferiti", description = "Endpoint degli itinerari preferiti degli utenti")
public class PreferitoController {

    @Autowired
    private PreferitoService service;

    @GetMapping
    @Operation(
            summary = "restituisce la lista dei preferiti dell'utente",
            description = "permette di vedere quali sono i preferiti dell'utente loggato"
    )
    public ResponseEntity<?> getPreferiti(){
        PreferitoDTO dto = service.getPreferiti();

        if(dto != null){
            return ResponseEntity.ok(dto);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @PostMapping
    @Operation(
            summary = "aggiunge itinerario alla lista dei preferiti",
            description = "riceve un itinerario e lo aggiunge alla lista degli itinerari nell'entita' Preferiti"
    )
    public ResponseEntity<?> addItinerarioNeiPreferiti(@RequestBody ItinerarioDTO itinerario){
        service.addPreferito(itinerario);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @DeleteMapping
    @Operation(
            summary = "rimuove itinerario dalla lista dei preferiti",
            description = "riceve un itinerario e lo rimuove dalla lista degli itinerari nell'entita' Preferiti"
    )
    public ResponseEntity<?> removeItinerarioDaPreferiti(@RequestBody ItinerarioDTO itinerario){
        service.removePreferito(itinerario);
        return ResponseEntity.ok(null);
    }
}
