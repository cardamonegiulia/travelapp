package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneDTO;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recensioni")
public class RecensioneController {

    @Autowired
    private RecensioneService service;

    @GetMapping("/{id}")
    public ResponseEntity<RecensioneDTO> getById(@PathVariable Long id){

        RecensioneDTO dto = service.getById(id);

        if(dto != null){
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @PostMapping("/addRecensione")
    public boolean addNewRecensione(Prenotazione prenotazione, int voto, String commento){
        RecensioneDTO dto = new RecensioneDTO();
        dto.setVotazione(voto);
        dto.setComm(commento);
        dto.setPreno(prenotazione);

        return  service.addNewRecensione(dto);
    }
}
