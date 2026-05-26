package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.experience.exeption.ItinerarioNonTrovato;
import com.unical.travelapp.backend.experience.models.DTO.PreferitoDTO;
import com.unical.travelapp.backend.experience.models.Preferito;
import com.unical.travelapp.backend.experience.repository.PreferitoRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
                preDTO.setUtenteId(preferito.getUtente().getId());
                preDTO.setItinerariList(
                    preferito.getItinerario().stream()
                        .map(itin -> {
                            ItinerarioDTO dto = new ItinerarioDTO();
                            dto.setId(itin.getId());
                            dto.setOrganizzatoreId(itin.getOrganizzatore().getId());
                            dto.setTitolo(itin.getTitolo());
                            dto.setDescrizione(itin.getDescrizione());
                            dto.setDestinazionePrincipale(itin.getDestinazionePrincipale());
                            dto.setPrezzoBase(itin.getPrezzoBase());
                            dto.setDurataGiorni(itin.getDurataGiorni());
                            dto.setMaxPartecipanti(itin.getMaxPartecipanti());
                            dto.setStato(itin.getStato());
                            return dto;
                        })
                        .toList()
                );
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

    @Transactional
    public void removePreferito(ItinerarioDTO itinerarioDTO){
        Utente utente = utenteService.getUtenteSessione();

        if (utente != null){
            Preferito preferiti = repo.findByUtente(utente);
            Itinerario itinerario = itinerarioRepository.findById(itinerarioDTO.getId())
                    .orElseThrow(() -> new ItinerarioNonTrovato("itinerario non trovato"));

            preferiti.getItinerario().remove(itinerario);
        }
    }
}
