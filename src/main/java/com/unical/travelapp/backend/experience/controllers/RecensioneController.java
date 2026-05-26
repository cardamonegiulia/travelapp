package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.experience.models.DTO.RecensioneDTO;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recensioni")
@Tag(name = "Gestione Recensioni", description = "Endpoint per le recensioni degli utenti")
public class RecensioneController {

    @Autowired
    private RecensioneService service;

    @GetMapping("/{id}")
    @Operation(
            summary = "restituisce la recensione tramite id"
    )
    public ResponseEntity<RecensioneDTO> getById(@PathVariable Long id){

        RecensioneDTO dto = service.getById(id);

        if(dto != null){
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @PostMapping
    @Operation(
            summary = "aggiunge una nuova recensione",
            description = "riceve in input il DTO con prenotazioneId, voto e commento e lo passa al service"
    )
    public ResponseEntity<?> addNewRecensione(@RequestBody RecensioneDTO dto) {
        service.addNewRecensione(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Recensione aggiunta con successo!");
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "rimuove una recensione",
            description = "riceve l'id della recensione e la rimuove"
    )
    public ResponseEntity<?> removeRecensione(@PathVariable Long id){
        service.deleteRecensione(id);
        return ResponseEntity.ok().build();
    }
}
