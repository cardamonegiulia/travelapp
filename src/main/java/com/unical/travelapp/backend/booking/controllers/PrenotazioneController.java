package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.dto.PartenzaOrganizzatoreDto;
import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.mapper.PrenotazioneAssembler;
import com.unical.travelapp.backend.booking.service.PrenotazioneService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/prenotazioni")
@AllArgsConstructor
@Tag(name = "Prenotazioni", description = "Gestione delle prenotazioni di viaggi e attività")
@SecurityRequirement(name = "bearerAuth")
public class PrenotazioneController {

    private final PrenotazioneService prenotazioneService;
    private final PrenotazioneAssembler prenotazioneAssembler;
    private final AuditLogger auditLogger;

    @GetMapping("/{id}")
    @Operation(
            summary = "Restituisce una prenotazione per id",
            description = "Accessibile solo dal proprietario della prenotazione o da un ADMIN. Restituisce 404 se la prenotazione non appartiene all'utente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prenotazione trovata"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Prenotazione non trovata")
    })
    public ResponseEntity<PrenotazioneResponseDto> getPrenotazione(@PathVariable Long id) {
        return ResponseEntity.ok(
                prenotazioneAssembler.assembla(prenotazioneService.getPrenotazioneById(id)));
    }

    @GetMapping("/utente/{utenteId}")
    @Operation(
            summary = "Restituisce tutte le prenotazioni di un utente (paginato)",
            description = "Accessibile solo dal proprietario o da un ADMIN. Restituisce 403 se si tenta di accedere alle prenotazioni di un altro utente. Ordinate per data decrescente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista prenotazioni restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — non puoi vedere le prenotazioni di un altro utente")
    })
    public ResponseEntity<Page<PrenotazioneResponseDto>> getPrenotazioniByUtente(
            @PathVariable Long utenteId,
            @PageableDefault(size = 20, sort = "dataPrenotazione", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(prenotazioneAssembler.assembla(prenotazioneService.getPrenotazioniByUtente(utenteId, pageable)));
    }

    @GetMapping("/mie")
    @Operation(
            summary = "Restituisce le prenotazioni dell'utente loggato (paginato)",
            description = "Accessibile da qualsiasi utente autenticato. Restituisce sempre e solo le prenotazioni dell'utente loggato. Ordinate per data decrescente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista prenotazioni restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Page<PrenotazioneResponseDto>> getMiePrenotazioni(
            @PageableDefault(size = 20, sort = "dataPrenotazione", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(prenotazioneAssembler.assembla(prenotazioneService.getMiePrenotazioni(pageable)));
    }

    @GetMapping("/mie/attuali")
    @Operation(
            summary = "Prenotazioni dell'utente loggato ancora da concludere (paginato)",
            description = "Viaggi in corso o futuri, piu' le prenotazioni cancellate. E' la lista "
                    + "storica della sezione Prenotazioni: cambia solo perche' i viaggi gia' conclusi "
                    + "vengono ora mostrati nella scheda dedicata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista prenotazioni restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Page<PrenotazioneResponseDto>> getMiePrenotazioniAttuali(
            @PageableDefault(size = 20, sort = "dataPrenotazione", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(prenotazioneAssembler.assembla(prenotazioneService.getMieAttuali(pageable)));
    }

    @GetMapping("/mie/concluse")
    @Operation(
            summary = "Viaggi conclusi dell'utente loggato (paginato)",
            description = "Prenotazioni non cancellate la cui data di fine e' gia' passata, dalla piu' "
                    + "recente. Ogni elemento porta con se' se il viaggio e' ancora recensibile e "
                    + "l'eventuale recensione gia' scritta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista viaggi conclusi restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Page<PrenotazioneResponseDto>> getMieiViaggiConclusi(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(prenotazioneAssembler.assembla(prenotazioneService.getMieConcluse(pageable)));
    }

    @PostMapping
    @Operation(
            summary = "Crea una nuova prenotazione",
            description = "Accessibile da qualsiasi utente autenticato. L'utente può prenotare solo per se stesso."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prenotazione creata con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "409", description = "Disponibilità esaurita o prenotazione già esistente")
    })
    public ResponseEntity<PrenotazioneResponseDto> creaPrenotazione(
            @Valid @RequestBody CreaPrenotazioneRequest request) {
        Prenotazione prenotazione = prenotazioneService.createPrenotazione(request);
        auditLogger.success("PRENOTAZIONE_CREATA", "Prenotazione", String.valueOf(prenotazione.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(prenotazioneAssembler.assembla(prenotazione));
    }

    @PostMapping("/{id}/annulla")
    @Operation(
            summary = "Annulla una prenotazione",
            description = "Accessibile solo dal proprietario della prenotazione o da un ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prenotazione annullata con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Prenotazione non trovata"),
            @ApiResponse(responseCode = "409", description = "Stato prenotazione non valido per l'annullamento")
    })
    public ResponseEntity<PrenotazioneResponseDto> annullaPrenotazione(@PathVariable Long id) {
        Prenotazione prenotazione = prenotazioneService.annullaPrenotazione(id);
        auditLogger.success("PRENOTAZIONE_ANNULLATA", "Prenotazione", String.valueOf(id));
        return ResponseEntity.ok(prenotazioneAssembler.assembla(prenotazione));
    }

    @GetMapping("/organizzatore/itinerari/{itinerarioId}/partenze")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(
            summary = "Partenze ancora da fare di un proprio itinerario, con le vendite",
            description = "Riservato all'organizzatore che ha creato l'itinerario (un ADMIN vede "
                    + "qualsiasi itinerario). Le partenze gia' concluse non vengono restituite; "
                    + "quelle in corso si'. Ordinate dalla piu' vicina."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Elenco partenze restituito con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Itinerario inesistente o di un altro organizzatore")
    })
    public ResponseEntity<List<PartenzaOrganizzatoreDto>> getPartenzeItinerario(
            @PathVariable Long itinerarioId) {
        return ResponseEntity.ok(prenotazioneService.getPartenzeFuture(itinerarioId));
    }

    @GetMapping("/organizzatore/partenze/{disponibilitaId}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    @Operation(
            summary = "Viaggiatori prenotati su una partenza (paginato)",
            description = "Riservato all'organizzatore dell'itinerario a cui la partenza appartiene "
                    + "(un ADMIN vede qualsiasi partenza). Le prenotazioni cancellate non compaiono."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista prenotati restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Partenza inesistente o di un altro organizzatore")
    })
    public ResponseEntity<Page<PrenotazioneResponseDto>> getPrenotatiPartenza(
            @PathVariable Long disponibilitaId,
            @PageableDefault(size = 50, sort = "dataPrenotazione", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(prenotazioneAssembler.assembla(
                prenotazioneService.getPrenotazioniPerPartenza(disponibilitaId, pageable)));
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
