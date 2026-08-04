package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.experience.models.DTO.RecensioneRequest;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneResponse;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recensioni")
@Tag(name = "Gestione Recensioni", description = "Endpoint per le recensioni degli utenti")
public class RecensioneController {

    @Autowired
    private RecensioneService service;

    @GetMapping("/{id}")
    @Operation(summary = "Restituisce una recensione tramite il suo ID")
    public ResponseEntity<RecensioneResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }


    @GetMapping("/itinerario/{itinerarioId}")
    @Operation(
            summary = "Restituisce tutte le recensioni di un itinerario",
            description = "Dato l'ID di un itinerario, ritorna la lista di tutte le recensioni collegate. " +
                    "Restituisce lista vuota se non ce ne sono"
    )
    public ResponseEntity<List<RecensioneResponse>> leggiRecensioniItinerario(@PathVariable Long itinerarioId) {
        return ResponseEntity.ok(service.getRecensioniDaItinerarioId(itinerarioId));
    }


    @GetMapping("/itinerario/{itinerarioId}/media")
    @Operation(
            summary = "Restituisce la media dei voti di un itinerario",
            description = "Calcola la media aritmetica dei voti. Restituisce 0.0 se non ci sono recensioni"
    )
    public ResponseEntity<Double> getMediaVoti(@PathVariable Long itinerarioId) {
        return ResponseEntity.ok(service.getMediaVoti(itinerarioId));
    }


    @PostMapping
    @Operation(
            summary = "Aggiunge una nuova recensione",
            description = "Riceve il DTO con itinerarioId (o prenotazioneId), voto e commento. " +
                    "Se viene passata la prenotazione, verifica che appartenga all'utente e che non sia gia' stata recensita"
    )
    public ResponseEntity<?> addNewRecensione(@Valid @RequestBody RecensioneRequest dto) {
        service.addNewRecensione(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Recensione aggiunta con successo!");
    }


    @DeleteMapping("/{id}")
    @Operation(
            summary = "Rimuove una recensione",
            description = "Elimina la recensione con l'ID specificato. Solo l'autore puo' cancellarla"
    )
    public ResponseEntity<?> removeRecensione(@PathVariable Long id) {
        service.deleteRecensione(id);
        return ResponseEntity.ok().build();
    }
}
