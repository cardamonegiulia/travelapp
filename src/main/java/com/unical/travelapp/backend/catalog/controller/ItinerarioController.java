package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.mapper.ItinerarioMapper;
import com.unical.travelapp.backend.catalog.service.ItinerarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/itinerari")
public class ItinerarioController {

    @Autowired
    private ItinerarioService itinerarioService;

    @Autowired
    private ItinerarioMapper itinerarioMapper;

    @GetMapping
    public ResponseEntity<List<ItinerarioDTO>> getAllItinerari() {
        List<ItinerarioDTO> dtos = itinerarioService.getAllItinerari().stream()
                .map(itinerarioMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItinerarioDTO> getItinerarioById(@PathVariable Long id) {
        Optional<Itinerario> itinerario = itinerarioService.getItinerarioById(id);
        return itinerario.map(it -> ResponseEntity.ok(itinerarioMapper.toDTO(it)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ItinerarioDTO> createItinerario(@RequestBody ItinerarioDTO itinerarioDTO) {
        Itinerario entity = itinerarioMapper.toEntity(itinerarioDTO);
        Itinerario salvato = itinerarioService.saveItinerario(entity);
        return ResponseEntity.ok(itinerarioMapper.toDTO(salvato));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItinerario(@PathVariable Long id) {
        itinerarioService.deleteItinerario(id);
        return ResponseEntity.noContent().build();
    }
}