package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.booking.service.PrenotazioneService;
import com.unical.travelapp.backend.experience.exeption.PrenotazioneNonTrovata;
import com.unical.travelapp.backend.experience.exeption.RecensioneNonTrovata;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneDTO;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.experience.repository.RecensioneRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RecensioneService {

    @Autowired
    private RecensioneRepository repo;
    private UtenteService utenteService;
    private PrenotazioneRepository prenotazioneRepository;

    public RecensioneDTO getById(Long id){
        RecensioneDTO dto = null;

        Optional<Recensione> recensione = repo.findById(id);

        try {
            if (recensione != null) {
                dto = new RecensioneDTO();
                dto.setPreno(recensione.get().getPrenotazione());
                dto.setUt(recensione.get().getUtente());
                dto.setComm(recensione.get().getCommento());
                dto.setVotazione(recensione.get().getVoto());
            }
        }
        catch (Exception e){
            System.out.println(e);
        }

        return dto;
    }

    public void addNewRecensione(RecensioneDTO dto) {

        Utente utente = utenteService.getUtenteSessione();

        Prenotazione prenotazione = prenotazioneRepository
                .findById(dto.getPrenotazioneId())
                .orElseThrow(() -> new PrenotazioneNonTrovata("Prenotazione non trovata"));

        if (!prenotazione.getViaggiatore().getId().equals(utente.getId())) {
            throw new SecurityException("Non autorizzato a recensire questa prenotazione");
        }

        Recensione recensione = new Recensione();
        recensione.setUtente(utente);
        recensione.setPrenotazione(prenotazione);
        recensione.setCommento(dto.getComm());
        recensione.setVoto(dto.getVotazione());

        repo.save(recensione);
    }

    public void deleteRecensione(Long id){
        Recensione recensione = repo.findById(id)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata"));

        repo.delete(recensione);
    }
}
