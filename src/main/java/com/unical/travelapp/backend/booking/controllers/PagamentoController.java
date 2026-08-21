package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.dto.PagamentoResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.mapper.PagamentoMapper;
import com.unical.travelapp.backend.booking.service.PagamentoService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/pagamenti")
@AllArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;
    private final PagamentoMapper pagamentoMapper;
    private final AuditLogger auditLogger;

    // Completa il pagamento associato alla prenotazione.
    // I controlli sull'utente e sugli stati vengono gestiti dal service.
    @PostMapping("/prenotazioni/{prenotazioneId}/paga")
    public ResponseEntity<PagamentoResponseDto> pagaPrenotazione(
            @PathVariable Long prenotazioneId) {

        Pagamento pagamento =
                pagamentoService.pagaPrenotazione(prenotazioneId);

        auditLogger.success(
                "PRENOTAZIONE_PAGATA",
                "Prenotazione",
                String.valueOf(prenotazioneId)
        );

        PagamentoResponseDto responseDto =
                pagamentoMapper.toResponseDto(pagamento);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(responseDto);
    }
    @GetMapping("/miei")
    public ResponseEntity<Page<PagamentoResponseDto>> getMieiPagamenti(
            @PageableDefault(size = 20, sort = "prenotazione.dataPrenotazione",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Pagamento> pagamenti =
                pagamentoService.getPagamentiUtente(pageable);

        Page<PagamentoResponseDto> response =
                pagamenti.map(pagamentoMapper::toResponseDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}