package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.experience.exeption.ItinerarioNonTrovato;
import com.unical.travelapp.backend.experience.exeption.PrenotazioneNonTrovata;
import com.unical.travelapp.backend.experience.exeption.RecensioneNonTrovata;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneDTO;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.experience.repository.RecensioneRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecensioneService {

    @Autowired
    private RecensioneRepository repo;

    @Autowired
    private UtenteService utenteService;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    @Autowired
    private ItinerarioRepository itinerarioRepository;


    public RecensioneDTO getById(Long id) {
        Recensione recensione = repo.findById(id)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata con id: " + id));
        return toDTO(recensione);
    }


    public List<RecensioneDTO> getRecensioniDaItinerarioId(Long itinerarioId) {
        return repo.findByItinerario_Id(itinerarioId)
                .stream()
                .map(this::toDTO)
                .toList();
    }


    public double getMediaVoti(Long itinerarioId) {
        List<Recensione> recensioni = repo.findByItinerario_Id(itinerarioId);
        return recensioni.stream()
                .mapToInt(Recensione::getVoto)
                .average()
                .orElse(0.0);
    }


    public void addNewRecensione(RecensioneDTO dto) {

        Utente utente = utenteService.getUtenteSessione();

        // Ricava l'itinerario: prima dal campo diretto, poi dalla prenotazione
        Itinerario itinerario = resolveItinerario(dto);

        // Gestione prenotazione (opzionale)
        Prenotazione prenotazione = null;
        if (dto.getPrenotazioneId() != null) {
            prenotazione = prenotazioneRepository
                    .findById(dto.getPrenotazioneId())
                    .orElseThrow(() -> new PrenotazioneNonTrovata("Prenotazione non trovata"));

            if (!prenotazione.getViaggiatore().getId().equals(utente.getId())) {
                throw new SecurityException("Non autorizzato a recensire questa prenotazione");
            }

            if (repo.existsByPrenotazione(prenotazione)) {
                throw new IllegalStateException("Hai gia' recensito questa prenotazione");
            }
        }

        Recensione recensione = new Recensione();
        recensione.setUtente(utente);
        recensione.setPrenotazione(prenotazione);
        recensione.setItinerario(itinerario);
        recensione.setCommento(dto.getComm());
        recensione.setVoto(dto.getVotazione());

        repo.save(recensione);
    }



    public void deleteRecensione(Long id) {
        Recensione recensione = repo.findById(id)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata"));

        Utente utente = utenteService.getUtenteSessione();
        if (!recensione.getUtente().getId().equals(utente.getId())) {
            throw new SecurityException("Non autorizzato a eliminare questa recensione");
        }

        repo.delete(recensione);
    }


    private RecensioneDTO toDTO(Recensione r) {
        RecensioneDTO dto = new RecensioneDTO();
        dto.setUtenteId(r.getUtente().getId());
        dto.setComm(r.getCommento());
        dto.setVotazione(r.getVoto());

        if (r.getItinerario() != null) {
            dto.setItinerarioId(r.getItinerario().getId());
        }
        if (r.getPrenotazione() != null) {
            dto.setPrenotazioneId(r.getPrenotazione().getId());
        }
        return dto;
    }


    private Itinerario resolveItinerario(RecensioneDTO dto) {
        if (dto.getItinerarioId() != null) {
            return itinerarioRepository.findById(dto.getItinerarioId())
                    .orElseThrow(() -> new ItinerarioNonTrovato("Itinerario non trovato"));
        }

        if (dto.getPrenotazioneId() != null) {
            Optional<Prenotazione> prenotazione = prenotazioneRepository.findById(dto.getPrenotazioneId());
            if (prenotazione.isPresent() && prenotazione.get().getDisponibilitaItinerario() != null) {
                return prenotazione.get().getDisponibilitaItinerario().getItinerario();
            }
        }

        throw new IllegalArgumentException("Fornire itinerarioId oppure prenotazioneId valido con disponibilita'");
    }
}
