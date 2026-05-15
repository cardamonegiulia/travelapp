package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtenteService {

    private final UtenteRepository utenteRepository;

    // Iniezione delle dipendenze
    public UtenteService(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    public Utente salvaUtente(Utente utente) {
        // Qui andranno i controlli
        return utenteRepository.save(utente);
    }

    public List<Utente> ottieniTutti() {
        return utenteRepository.findAll();
    }
}