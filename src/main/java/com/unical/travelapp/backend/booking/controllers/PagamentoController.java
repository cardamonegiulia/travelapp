package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.dto.PagamentoResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.mapper.PagamentoMapper;
import com.unical.travelapp.backend.booking.service.PagamentoService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamenti")
@AllArgsConstructor
@Tag(name = "Pagamenti", description = "Gestione dei pagamenti delle prenotazioni")
@SecurityRequirement(name = "bearerAuth")
public class PagamentoController {

    private final PagamentoService pagamentoService;
    private final PagamentoMapper pagamentoMapper;
    private final AuditLogger auditLogger;

    @PostMapping("/prenotazioni/{prenotazioneId}/paga")
    @Operation(
            summary = "Completa il pagamento di una prenotazione",
            description = "Accessibile solo dal proprietario della prenotazione. I controlli sull'utente e sugli stati vengono gestiti dal service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento completato con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo il proprietario può pagare"),
            @ApiResponse(responseCode = "404", description = "Prenotazione non trovata"),
            @ApiResponse(responseCode = "409", description = "Stato pagamento non valido per questa operazione")
    })
    public ResponseEntity<PagamentoResponseDto> pagaPrenotazione(
            @PathVariable Long prenotazioneId) {
        Pagamento pagamento = pagamentoService.pagaPrenotazione(prenotazioneId);
        auditLogger.success("PRENOTAZIONE_PAGATA", "Prenotazione", String.valueOf(prenotazioneId));
        return ResponseEntity.status(HttpStatus.OK).body(pagamentoMapper.toResponseDto(pagamento));
    }

    @GetMapping("/miei")
    @Operation(
            summary = "Restituisce i pagamenti dell'utente autenticato (paginati)",
            description = "Accessibile da qualsiasi utente autenticato. Restituisce solo i pagamenti dell'utente loggato, ordinati per data prenotazione decrescente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista pagamenti restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Page<PagamentoResponseDto>> getMieiPagamenti(
            @PageableDefault(size = 20, sort = "prenotazione.dataPrenotazione",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Pagamento> pagamenti = pagamentoService.getPagamentiUtente(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(pagamenti.map(pagamentoMapper::toResponseDto));
    }
}