package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.services.ImmagineService;
import com.unical.travelapp.backend.experience.services.ImmagineService.ContenutoImmagine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/immagini")
@Tag(name = "Gestione Immagini", description = "Caricamento e recupero delle immagini degli utenti")
@SecurityRequirement(name = "bearerAuth")
public class ImmagineController {

    private final ImmagineService service;
    private final AuditLogger auditLogger;

    public ImmagineController(ImmagineService service, AuditLogger auditLogger) {
        this.service = service;
        this.auditLogger = auditLogger;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Carica una nuova immagine",
            description = "Accetta un file JPEG o PNG di al massimo 5 MB inviato come multipart/form-data. " +
                    "Il file viene validato (dimensione, estensione, tipo reale del contenuto), " +
                    "rinominato con un UUID e salvato sullo storage configurato. " +
                    "Restituisce i metadati con l'URL da cui scaricarlo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Immagine caricata con successo"),
            @ApiResponse(responseCode = "400", description = "File non valido — tipo non supportato o dimensione superiore a 5 MB"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<ImmagineResponse> carica(@RequestParam("file") MultipartFile file) {
        ImmagineResponse immagine = service.carica(file);
        auditLogger.success("IMMAGINE_CARICATA", "Immagine", String.valueOf(immagine.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(immagine);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Restituisce i metadati di un'immagine tramite il suo ID",
            description = "Accessibile da qualsiasi utente autenticato."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadati immagine restituiti con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Immagine non trovata")
    })
    public ResponseEntity<ImmagineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/mie")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Restituisce le immagini caricate dall'utente in sessione",
            description = "Lista paginata. Restituisce una pagina vuota se l'utente non ha caricato nulla."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista immagini restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<Page<ImmagineResponse>> getMie(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getMie(pageable));
    }

    @GetMapping("/{id}/contenuto")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Scarica il contenuto binario di un'immagine",
            description = "Restituisce il file binario con il Content-Type corretto rilevato al momento dell'upload. " +
                    "Il file è cacheable lato client per 1 ora ma non nelle cache condivise."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contenuto immagine restituito con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Immagine non trovata")
    })
    public ResponseEntity<Resource> getContenuto(@PathVariable Long id) {
        ContenutoImmagine contenuto = service.getContenuto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contenuto.contentType()))
                .contentLength(contenuto.dimensioneByte())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + contenuto.nomeFile() + "\"")
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(contenuto.risorsa());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Elimina un'immagine",
            description = "Rimuove metadati e file. Consentito solo al proprietario dell'immagine o agli ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Immagine eliminata con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti — solo il proprietario o ADMIN"),
            @ApiResponse(responseCode = "404", description = "Immagine non trovata")
    })
    public ResponseEntity<Void> elimina(@PathVariable Long id) {
        service.elimina(id);
        auditLogger.success("IMMAGINE_ELIMINATA", "Immagine", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}