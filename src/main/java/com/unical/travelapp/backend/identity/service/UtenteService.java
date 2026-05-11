package com.unical.travelapp.backend.identity.service;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtenteService {

    private final UtenteRepository utenteRepository;

    public UtenteService(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    // Metodo per salvare un nuovo utente su Neon
    public Utente creaUtente(Utente utente) {
        return utenteRepository.save(utente);
    }

    // Metodo per leggere tutti gli utenti da Neon
    public List<Utente> getAllUtenti() {
        return utenteRepository.findAll();
    }
}