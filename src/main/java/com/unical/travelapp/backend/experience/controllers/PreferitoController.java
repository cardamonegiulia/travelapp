package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.models.DTO.PreferitoDTO;
import com.unical.travelapp.backend.experience.models.Preferito;
import com.unical.travelapp.backend.experience.repository.PreferitoRepository;
import com.unical.travelapp.backend.experience.services.PreferitoService;
import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/preferiti")
public class PreferitoController {

    @Autowired
    private PreferitoService service;

    public ResponseEntity<?> getPreferiti(){
        PreferitoDTO dto = service.getPreferiti();

        if(dto != null){
            return ResponseEntity.ok(dto);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    public ResponseEntity<?> addItinerarioNeiPreferiti(ItinerarioDTO itinerario){
        service.addPreferito(itinerario);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
    }
}
