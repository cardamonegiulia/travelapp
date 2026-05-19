package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.service.PrenotazioneService;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prenotazioni")
@AllArgsConstructor
public class PrenotazioneController {
    private final PrenotazioneService prenotazioneService;

    @PostMapping
    public ResponseEntity<Prenotazione> creaPrenotazione(@RequestBody CreaPrenotazioneRequest request) {
        Prenotazione prenotazione = prenotazioneService.createPrenotazione(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(prenotazione);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prenotazione> getPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.getPrenotazioneById(id);
        return ResponseEntity.status(HttpStatus.OK).body(prenotazione);
    }

    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<List<Prenotazione>> getPrenotazioniByIdUtente(@PathVariable Long utenteId) {
        List<Prenotazione> prenotazioni = prenotazioneService.getPrenotazioniByUtente(utenteId);
        return ResponseEntity.status(HttpStatus.OK).body(prenotazioni);
    }

    @PostMapping("/{id}/paga")
    public ResponseEntity<Prenotazione> pagaPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.pagaPrenotazione(id);
        return ResponseEntity.status(HttpStatus.OK).body(prenotazione);
    }

    @PostMapping("/{id}/annulla")
    public ResponseEntity<Prenotazione> annullaPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.annullaPrenotazione(id);
        return ResponseEntity.status(HttpStatus.OK).body(prenotazione);
    }








}
