package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.mapper.PrenotazioneMapper;
import com.unical.travelapp.backend.booking.service.PrenotazioneService;
import com.unical.travelapp.backend.booking.service.PagamentoService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prenotazioni")
@AllArgsConstructor
public class PrenotazioneController {

    private final PrenotazioneService prenotazioneService;
    private final PrenotazioneMapper prenotazioneMapper;
    private final AuditLogger auditLogger;
    private final PagamentoService pagamentoService;

    @GetMapping("/{id}")
    public ResponseEntity<PrenotazioneResponseDto> getPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.getPrenotazioneById(id);
        Pagamento pagamento = pagamentoService.getPagamentoPrenotazione(prenotazione.getId());
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione, pagamento);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/utente/{utenteId}")
    public ResponseEntity<Page<PrenotazioneResponseDto>> getPrenotazioniByUtente(@PathVariable Long utenteId, @PageableDefault(size = 20, sort = "dataPrenotazione", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Prenotazione> prenotazioni = prenotazioneService.getPrenotazioniByUtente(utenteId, pageable);
        List<Long> ids = prenotazioni.getContent().stream().map(Prenotazione::getId).toList();
        Map<Long, Pagamento> pagamenti = pagamentoService.getPagamentiPerPrenotazioni(ids);
        Page<PrenotazioneResponseDto> response = prenotazioneMapper.toResponseDtoPage(prenotazioni, pagamenti);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mie")
    public ResponseEntity<Page<PrenotazioneResponseDto>> getMiePrenotazioni(@PageableDefault(size = 20, sort = "dataPrenotazione", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Prenotazione> prenotazioni = prenotazioneService.getMiePrenotazioni(pageable);
        List<Long> ids = prenotazioni.getContent().stream().map(Prenotazione::getId).toList();
        Map<Long, Pagamento> pagamenti = pagamentoService.getPagamentiPerPrenotazioni(ids);
        Page<PrenotazioneResponseDto> response = prenotazioneMapper.toResponseDtoPage(prenotazioni, pagamenti);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PrenotazioneResponseDto> creaPrenotazione(@Valid @RequestBody CreaPrenotazioneRequest request) {
        Prenotazione prenotazione = prenotazioneService.createPrenotazione(request);
        auditLogger.success("PRENOTAZIONE_CREATA", "Prenotazione", String.valueOf(prenotazione.getId()));
        Pagamento pagamento = pagamentoService.getPagamentoPrenotazione(prenotazione.getId());
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione, pagamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/{id}/annulla")
    public ResponseEntity<PrenotazioneResponseDto> annullaPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.annullaPrenotazione(id);
        auditLogger.success("PRENOTAZIONE_ANNULLATA", "Prenotazione", String.valueOf(id));
        Pagamento pagamento = pagamentoService.getPagamentoPrenotazione(prenotazione.getId());
        PrenotazioneResponseDto responseDto = prenotazioneMapper.toResponseDto(prenotazione, pagamento);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/saldo/totale")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ottiene il saldo globale totale della piattaforma")
    public ResponseEntity<BigDecimal> getSaldoTotaleGlobale() {
        return ResponseEntity.ok(prenotazioneService.getSaldoTotaleGlobale());
    }

    @GetMapping("/saldo/organizzatore")
    @PreAuthorize("hasRole('ORGANIZZATORE') or hasRole('ADMIN')")
    @Operation(summary = "Ottiene il saldo incassato dall'organizzatore autenticato")
    public ResponseEntity<BigDecimal> getSaldoOrganizzatore() {
        return ResponseEntity.ok(prenotazioneService.getSaldoOrganizzatore());
    }
}