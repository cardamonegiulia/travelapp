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

@Service
public class PreferitoService {

    @Autowired
    private UtenteService utenteService;
    @Autowired
    private PreferitoRepository repo;
    @Autowired
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
    public void addPreferito(Long itinerarioId){
        Utente utente = utenteService.getUtenteSessione();

        if (utente != null){
            Preferito prefe = repo.findByUtente(utente);
            if(prefe == null){
                prefe = new Preferito();
                prefe.setUtente(utente);
            }
            Itinerario itin = itinerarioRepository.findById(itinerarioId)
                    .orElseThrow(() -> new ItinerarioNonTrovato("itinerario non trovato"));

            prefe.getItinerario().add(itin);
            repo.save(prefe);
        }
    }

    @Transactional
    public void removePreferito(Long itinerarioId){
        Utente utente = utenteService.getUtenteSessione();

        if (utente != null){
            Preferito preferiti = repo.findByUtente(utente);
            Itinerario itinerario = itinerarioRepository.findById(itinerarioId)
                    .orElseThrow(() -> new ItinerarioNonTrovato("itinerario non trovato"));

            if (preferiti != null) {
                preferiti.getItinerario().remove(itinerario);
                repo.save(preferiti);
            }
        }
    }
}
