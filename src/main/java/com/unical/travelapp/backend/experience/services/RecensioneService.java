package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
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

    public boolean addNewRecensione(RecensioneDTO dto) {

        try {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            Jwt jwt = (Jwt) authentication.getPrincipal();

            Utente utente = utenteService.ottieniUtenteDaToken(jwt);

            Prenotazione prenotazione = prenotazioneRepository
                    .findById(dto.getPrenotazioneId())
                    .orElseThrow();

            if (!prenotazione.getUtente().getId().equals(utente.getId())) {
                throw new RuntimeException("Non autorizzato");
            }

            Recensione recensione = new Recensione();
            recensione.setUtente(utente);
            recensione.setPrenotazione(prenotazione);
            recensione.setCommento(dto.getComm());
            recensione.setVoto(dto.getVotazione());

            repo.save(recensione);

            return true;

        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }
}
