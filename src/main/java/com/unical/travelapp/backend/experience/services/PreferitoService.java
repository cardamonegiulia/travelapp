package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.experience.exeption.ItinerarioNonTrovato;
import com.unical.travelapp.backend.experience.models.DTO.PreferitoDTO;
import com.unical.travelapp.backend.experience.models.Preferito;
import com.unical.travelapp.backend.experience.repository.PreferitoRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import com.unical.travelapp.backend.identity.service.UtenteService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PreferitoService {

    @Autowired
    private UtenteService utenteService;
    private PreferitoRepository repo;
    ItinerarioRepository itinerarioRepository;

    public PreferitoDTO getPreferiti(){
        Utente utente = utenteService.getUtenteSessione();

        if (utente != null) {
            Preferito preferito = repo.findByUtente(utente);

            if (preferito != null){
                PreferitoDTO preDTO = new PreferitoDTO();
                preDTO.setItinerarioList(preferito.getItinerario());
                preDTO.setUtente(preferito.getUtente());
                return preDTO;
            }
        }

        return null;
    }

    @Transactional
    public void addPreferito(ItinerarioDTO itinerario){
        Utente utente = utenteService.getUtenteSessione();

        if (utente != null){
            Preferito prefe = repo.findByUtente(utente);
            Itinerario itin = itinerarioRepository.findById(itinerario.getId())
                    .orElseThrow(() -> new ItinerarioNonTrovato("itinerario non trovato"));

            prefe.getItinerario().add(itin);
        }
    }
}
