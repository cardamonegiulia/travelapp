package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.experience.models.DTO.NotificaResponse;
import com.unical.travelapp.backend.experience.services.NotificaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notifiche in-app dell'utente autenticato.
 *
 * <p>Non esiste un endpoint "notifiche di un altro utente", nemmeno per gli ADMIN: il
 * destinatario e' sempre quello del token.
 */
@RestController
@RequestMapping("/api/notifiche")
@Tag(name = "Notifiche", description = "Notifiche in-app dell'utente autenticato")
@SecurityRequirement(name = "bearerAuth")
public class NotificaController {

    private final NotificaService service;

    public NotificaController(NotificaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Restituisce le notifiche dell'utente loggato (paginate)",
            description = "Dalla piu' recente. Restituisce sempre e solo le notifiche di chi chiama."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Elenco restituito con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Page<NotificaResponse>> leggiMieNotifiche(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getMieNotifiche(pageable));
    }

    @GetMapping("/non-lette")
    @Operation(
            summary = "Numero di notifiche non ancora lette",
            description = "Usato per il pallino sul campanello: una sola chiamata leggera invece dell'elenco completo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conteggio restituito con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Long> contaNonLette() {
        return ResponseEntity.ok(service.contaMieNonLette());
    }

    @PostMapping("/{id}/letta")
    @Operation(
            summary = "Segna una notifica come letta",
            description = "Consentito solo sulle proprie notifiche: su quelle altrui risponde 404, non 403, "
                    + "per non rivelare quali id esistono."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifica segnata come letta"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Notifica non trovata")
    })
    public ResponseEntity<NotificaResponse> segnaLetta(@PathVariable Long id) {
        return ResponseEntity.ok(service.segnaLetta(id));
    }
}
