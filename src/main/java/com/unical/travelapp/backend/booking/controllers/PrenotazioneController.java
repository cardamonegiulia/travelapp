package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prenotazioni")
@AllArgsConstructor
public class PrenotazioneController {
    private final PrenotazioneService prenotazioneService;
    private final PrenotazioneMapper prenotazioneMapper;
    private final AuditLogger auditLogger;

    @GetMapping("/{id}")
    public ResponseEntity<PrenotazioneResponseDto> getPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.getPrenotazioneById(id);
        Pagamento pagamento = prenotazioneService.getPagamentoPrenotazione(prenotazione.getId());
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione, pagamento);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<Page<PrenotazioneResponseDto>> getPrenotazioniByUtente(@PathVariable Long utenteId, @PageableDefault(size = 20) Pageable pageable) {
        Page<Prenotazione> prenotazioni = prenotazioneService.getPrenotazioniByUtente(utenteId, pageable);
        List<Long> ids = prenotazioni.getContent()
                .stream()
                .map(Prenotazione::getId)
                .toList();
        Map<Long, Pagamento> pagamenti = prenotazioneService.getPagamentiPerPrenotazioni(ids);
        Page<PrenotazioneResponseDto> response = prenotazioni.map(prenotazione -> prenotazioneMapper.toResponseDto(prenotazione, pagamenti.get(prenotazione.getId())));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PrenotazioneResponseDto> creaPrenotazione(@Valid @RequestBody CreaPrenotazioneRequest request) {
        Prenotazione prenotazione = prenotazioneService.createPrenotazione(request);
        auditLogger.success(
                "PRENOTAZIONE_CREATA",
                "Prenotazione",
                String.valueOf(prenotazione.getId()));
        Pagamento pagamento = prenotazioneService.getPagamentoPrenotazione(prenotazione.getId());
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione, pagamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }


    @PostMapping("/{id}/annulla")
    public ResponseEntity<PrenotazioneResponseDto> annullaPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.annullaPrenotazione(id);
        auditLogger.success(
                "PRENOTAZIONE_ANNULLATA",
                "Prenotazione",
                String.valueOf(id)
        );
        Pagamento pagamento = prenotazioneService.getPagamentoPrenotazione(prenotazione.getId());
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione, pagamento);
        return ResponseEntity.ok(responseDto);
    }
}
