package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.mapper.PrenotazioneMapper;
import com.unical.travelapp.backend.booking.service.PrenotazioneService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Prenotazioni", description = "Gestione delle prenotazioni di viaggi e attività")
@SecurityRequirement(name = "bearerAuth")
public class PrenotazioneController {
    private final PrenotazioneService prenotazioneService;
    private final PrenotazioneMapper prenotazioneMapper;
    private final AuditLogger auditLogger;

    @PostMapping
    @Operation(
            summary = "Crea una nuova prenotazione",
            description = "Crea una prenotazione per l'utente autenticato. L'utente può prenotare solo per se stesso."
    )
    public ResponseEntity<PrenotazioneResponseDto> creaPrenotazione(@Valid @RequestBody CreaPrenotazioneRequest request) {
        Prenotazione prenotazione = prenotazioneService.createPrenotazione(request);
        auditLogger.success("PRENOTAZIONE_CREATA", "Prenotazione", String.valueOf(prenotazione.getId()));
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }


    @GetMapping("/{id}")
    @Operation(
            summary = "Restituisce una prenotazione per id",
            description = "Accessibile solo dal proprietario della prenotazione o da un ADMIN. Restituisce 404 se la prenotazione non appartiene all'utente."
    )
    public ResponseEntity<PrenotazioneResponseDto> getPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.getPrenotazioneById(id);
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @GetMapping("/utente/{utenteId}")
    @Operation(
            summary = "Restituisce tutte le prenotazioni di un utente",
            description = "Accessibile solo dal proprietario o da un ADMIN. Restituisce 403 se si tenta di accedere alle prenotazioni di un altro utente."
    )
    public ResponseEntity<Page<PrenotazioneResponseDto>> getPrenotazioniByUtente(
            @PathVariable Long utenteId, @PageableDefault(size = 20) Pageable pageable) {
        Page<Prenotazione> prenotazioni = prenotazioneService.getPrenotazioniByUtente(utenteId, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(prenotazioni.map(prenotazioneMapper::toResponseDto));
    }

    @PostMapping("/{id}/paga")
    @Operation(
            summary = "Effettua il pagamento di una prenotazione",
            description = "Accessibile solo dal proprietario della prenotazione o da un ADMIN."
    )
    public ResponseEntity<PrenotazioneResponseDto> pagaPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.pagaPrenotazione(id);
        auditLogger.success("PRENOTAZIONE_PAGATA", "Prenotazione", String.valueOf(id));
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @PostMapping("/{id}/annulla")
    @Operation(
            summary = "Annulla una prenotazione",
            description = "Accessibile solo dal proprietario della prenotazione o da un ADMIN."
    )
    public ResponseEntity<PrenotazioneResponseDto> annullaPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.annullaPrenotazione(id);
        auditLogger.success("PRENOTAZIONE_ANNULLATA", "Prenotazione", String.valueOf(id));
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }
}
