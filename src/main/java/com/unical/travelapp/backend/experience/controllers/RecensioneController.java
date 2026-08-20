package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneRequest;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneResponse;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/recensioni")
@Tag(name = "Gestione Recensioni", description = "Endpoint per le recensioni degli utenti")
public class RecensioneController {

    @Autowired
    private RecensioneService service;

    @Autowired
    private AuditLogger auditLogger;

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
    public ResponseEntity<Page<RecensioneResponse>> leggiRecensioniItinerario(
            @PathVariable Long itinerarioId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getRecensioniDaItinerarioId(itinerarioId, pageable));
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
        Long id = service.addNewRecensione(dto);
        auditLogger.success("RECENSIONE_CREATA", "Recensione", String.valueOf(id));
        return ResponseEntity.status(HttpStatus.CREATED).body("Recensione aggiunta con successo!");
    }


    @DeleteMapping("/{id}")
    @Operation(
            summary = "Rimuove una recensione",
            description = "Elimina la recensione con l'ID specificato, insieme alle sue foto. " +
                    "Solo l'autore puo' cancellarla"
    )
    public ResponseEntity<?> removeRecensione(@PathVariable Long id) {
        service.deleteRecensione(id);
        auditLogger.success("RECENSIONE_ELIMINATA", "Recensione", String.valueOf(id));
        return ResponseEntity.ok().build();
    }


    // --- Foto della recensione -----------------------------------------------------------

    @PostMapping(value = "/{id}/immagini", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Allega una foto alla recensione",
            description = "Riceve il file nel campo \"file\" di una richiesta multipart/form-data. " +
                    "Consentito all'autore della recensione e agli amministratori"
    )
    public ResponseEntity<ImmagineResponse> aggiungiImmagine(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {

        ImmagineResponse immagine = service.aggiungiImmagine(id, file);
        auditLogger.success("IMMAGINE_AGGIUNTA", "Recensione", String.valueOf(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(immagine);
    }


    @GetMapping("/{id}/immagini")
    @Operation(
            summary = "Restituisce le foto di una recensione",
            description = "Lista completa e non paginata: il numero di foto per recensione e' " +
                    "limitato lato server (app.storage.immagini.max-per-risorsa)"
    )
    public ResponseEntity<List<ImmagineResponse>> getImmagini(@PathVariable Long id) {
        return ResponseEntity.ok(service.getImmagini(id));
    }


    @DeleteMapping("/{id}/immagini/{immagineId}")
    @Operation(
            summary = "Rimuove una foto dalla recensione",
            description = "Cancella la foto dalla recensione e dallo storage. " +
                    "Consentito all'autore della recensione e agli amministratori"
    )
    public ResponseEntity<Void> rimuoviImmagine(@PathVariable Long id, @PathVariable Long immagineId) {
        service.rimuoviImmagine(id, immagineId);
        auditLogger.success("IMMAGINE_RIMOSSA", "Recensione", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}
