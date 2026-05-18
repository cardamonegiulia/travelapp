package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.catalog.mapper.SingolaAttivitaMapper;
import com.unical.travelapp.backend.catalog.service.SingolaAttivitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attivita")
public class SingolaAttivitaController {

    @Autowired
    private SingolaAttivitaService attivitaService;

    @Autowired
    private SingolaAttivitaMapper attivitaMapper;

    @GetMapping
    public ResponseEntity<List<SingolaAttivitaDTO>> getAllAttivita() {
        List<SingolaAttivitaDTO> dtos = attivitaService.getAllAttivita().stream()
                .map(attivitaMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SingolaAttivitaDTO> getAttivitaById(@PathVariable Long id) {
        Optional<SingolaAttivita> attivita = attivitaService.getAttivitaById(id);
        return attivita.map(att -> ResponseEntity.ok(attivitaMapper.toDTO(att)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/con-sessioni")
    public ResponseEntity<SingolaAttivitaDTO> createAttivitaConSessioni(
            @RequestBody SingolaAttivitaDTO attivitaDTO,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inizio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fine,
            @RequestParam List<Integer> giorni) {

        SingolaAttivita entity = attivitaMapper.toEntity(attivitaDTO);
        SingolaAttivita salvata = attivitaService.saveAttivitaConSessioni(entity, inizio, fine, giorni);
        return ResponseEntity.ok(attivitaMapper.toDTO(salvata));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttivita(@PathVariable Long id) {
        attivitaService.deleteAttivita(id);
        return ResponseEntity.noContent().build();
    }
}