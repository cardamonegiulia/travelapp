package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.mapper.ItinerarioMapper;
import com.unical.travelapp.backend.catalog.service.ItinerarioService;
import com.unical.travelapp.backend.identity.service.UtenteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/itinerari")
public class ItinerarioController {

    @Autowired
    private ItinerarioService itinerarioService;

    @Autowired
    private ItinerarioMapper itinerarioMapper;

    @Autowired
    private UtenteService utenteService;

    @GetMapping
    public ResponseEntity<Page<ItinerarioDTO>> getAllItinerari(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(itinerarioService.getAllItinerari(pageable).map(itinerarioMapper::toDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItinerarioDTO> getItinerarioById(@PathVariable Long id) {
        Optional<Itinerario> itinerario = itinerarioService.getItinerarioById(id);
        return itinerario.map(it -> ResponseEntity.ok(itinerarioMapper.toDTO(it)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<ItinerarioDTO> createItinerario(@Valid @RequestBody ItinerarioRequestDTO itinerarioRequest) {
        Itinerario entity = itinerarioMapper.fromRequest(itinerarioRequest);
        // organizzatore e stato sono gestiti dal server, mai dal client
        entity.setOrganizzatore(utenteService.getUtenteSessione());
        entity.setStato("BOZZA");
        Itinerario salvato = itinerarioService.saveItinerario(entity);
        return ResponseEntity.ok(itinerarioMapper.toDTO(salvato));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZZATORE', 'ADMIN')")
    public ResponseEntity<Void> deleteItinerario(@PathVariable Long id) {
        itinerarioService.deleteItinerario(id, utenteService.getUtenteSessione(), utenteService.isAdmin());
        return ResponseEntity.noContent().build();
    }
}