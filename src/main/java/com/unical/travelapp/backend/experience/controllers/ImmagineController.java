package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.services.ImmagineService;
import com.unical.travelapp.backend.experience.services.ImmagineService.ContenutoImmagine;
import io.swagger.v3.oas.annotations.Operation;
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
public class ImmagineController {

    private final ImmagineService service;
    private final AuditLogger auditLogger;

    public ImmagineController(ImmagineService service, AuditLogger auditLogger) {
        this.service = service;
        this.auditLogger = auditLogger;
    }

    /**
     * Carica un'immagine. Il file arriva come multipart/form-data nel campo "file"; il
     * proprietario e' l'utente del token, non un parametro della richiesta.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Carica una nuova immagine",
            description = "Accetta un file JPEG o PNG di al massimo 5 MB inviato come multipart/form-data. "
                    + "Il file viene validato (dimensione, estensione, tipo reale del contenuto), "
                    + "rinominato con un UUID e salvato sullo storage configurato. "
                    + "Restituisce i metadati con l'URL da cui scaricarlo"
    )
    public ResponseEntity<ImmagineResponse> carica(@RequestParam("file") MultipartFile file) {
        ImmagineResponse immagine = service.carica(file);
        auditLogger.success("IMMAGINE_CARICATA", "Immagine", String.valueOf(immagine.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(immagine);
    }


    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Restituisce i metadati di un'immagine tramite il suo ID")
    public ResponseEntity<ImmagineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }


    @GetMapping("/mie")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Restituisce le immagini caricate dall'utente in sessione",
            description = "Lista paginata; restituisce una pagina vuota se l'utente non ha caricato nulla"
    )
    public ResponseEntity<Page<ImmagineResponse>> getMie(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getMie(pageable));
    }


    /**
     * Restituisce il contenuto binario. I file non sono serviti come risorse statiche:
     * passano di qui, cioe' dietro autenticazione, e con un Content-Type preso dal database
     * (quello riconosciuto dal contenuto reale in fase di upload), mai dedotto dall'URL.
     */
    @GetMapping("/{id}/contenuto")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Scarica il contenuto di un'immagine")
    public ResponseEntity<Resource> getContenuto(@PathVariable Long id) {
        ContenutoImmagine contenuto = service.getContenuto(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contenuto.contentType()))
                .contentLength(contenuto.dimensioneByte())
                // "attachment" no: l'immagine e' fatta per essere mostrata nell'app. Il nome
                // e' quello generato da noi (UUID), quindi non c'e' input dell'utente
                // nell'header.
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + contenuto.nomeFile() + "\"")
                // contenuto privato: puo' stare nella cache del client, mai in cache condivise
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(contenuto.risorsa());
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Elimina un'immagine",
            description = "Rimuove metadati e file. Consentito solo al proprietario dell'immagine e agli amministratori"
    )
    public ResponseEntity<Void> elimina(@PathVariable Long id) {
        service.elimina(id);
        auditLogger.success("IMMAGINE_ELIMINATA", "Immagine", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}
