package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.mapper.PrenotazioneMapper;
import com.unical.travelapp.backend.booking.service.PrenotazioneService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prenotazioni")
@AllArgsConstructor
public class PrenotazioneController {
    private final PrenotazioneService prenotazioneService;
    private final PrenotazioneMapper prenotazioneMapper;
    private final AuditLogger auditLogger;

    @PostMapping
    public ResponseEntity<PrenotazioneResponseDto> creaPrenotazione(@Valid @RequestBody CreaPrenotazioneRequest request) {
        Prenotazione prenotazione = prenotazioneService.createPrenotazione(request);
        auditLogger.success("PRENOTAZIONE_CREATA", "Prenotazione", String.valueOf(prenotazione.getId()));
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrenotazioneResponseDto> getPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.getPrenotazioneById(id);
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<Page<PrenotazioneResponseDto>> getPrenotazioniByUtente(
            @PathVariable Long utenteId, @PageableDefault(size = 20) Pageable pageable) {
        Page<Prenotazione> prenotazioni = prenotazioneService.getPrenotazioniByUtente(utenteId, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(prenotazioni.map(prenotazioneMapper::toResponseDto));
    }


    @PostMapping("/{id}/annulla")
    public ResponseEntity<PrenotazioneResponseDto> annullaPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.annullaPrenotazione(id);
        auditLogger.success("PRENOTAZIONE_ANNULLATA", "Prenotazione", String.valueOf(id));
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }
}
