package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneDTO;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import com.unical.travelapp.backend.identity.entity.Utente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recensioni")
@Tag(name = "Gestione Recensioni", description = "Endpoint per le recensioni degli utenti")
public class RecensioneController {

    @Autowired
    private RecensioneService service;

    @GetMapping("/{id}")
    @Operation(
            summary = "restituisce le recensioni dell'utente"
    )
    public ResponseEntity<RecensioneDTO> getById(@PathVariable Long id){

        RecensioneDTO dto = service.getById(id);

        if(dto != null){
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @PostMapping("/addRecensione")
    @Operation(
            summary = "aggiunge una nuova recensione",
            description = "prende in input una prenotazione, il voto e il commento, crea il DTO e lo passa al service"
    )
    public ResponseEntity<?> addNewRecensione(Prenotazione prenotazione, int voto, String commento) {

        RecensioneDTO dto = new RecensioneDTO();
        dto.setVotazione(voto);
        dto.setComm(commento);
        dto.setPreno(prenotazione);

        service.addNewRecensione(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body("Recensione aggiunta con successo!");
    }

    @Operation(
            summary = "rimuove le recensioni",
            description = "riceve l'id della recensione e la rimuove"
    )
    public ResponseEntity<?> removeRecensione(Long id){
        service.deleteRecensione(id);

        return ResponseEntity.ok().build();
    }
}
