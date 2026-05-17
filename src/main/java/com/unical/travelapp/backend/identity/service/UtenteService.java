package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtenteService {

    private final UtenteRepository utenteRepository;

    public UtenteService(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    public Utente salvaUtenteDatoDTO(UtenteDto dto) {

        // Controllo email duplicata
        if (utenteRepository.existsByEmail(dto.getEmail())) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con email: " + dto.getEmail()
            );
        }

        // Controllo keycloakId duplicato
        if (utenteRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con keycloakId: " + dto.getKeycloakId()
            );
        }

        Utente utente = new Utente();
        utente.setKeycloakId(dto.getKeycloakId());
        utente.setNome(dto.getNome());
        utente.setCognome(dto.getCognome());
        utente.setEmail(dto.getEmail());
        utente.setRuolo(dto.getRuolo() != null ? dto.getRuolo() : Ruolo.VIAGGIATORE);

        return utenteRepository.save(utente);
    }

    public List<Utente> ottieniTutti() {
        return utenteRepository.findAll();
    }

    public Utente ottieniPerId(Long id) {
        return utenteRepository.findById(id)
                .orElseThrow(() -> new UtenteNonTrovatoException(
                        "Utente con id " + id + " non trovato"
                ));
    }

    public Utente ottieniPerKeycloakId(String keycloakId) {
        return utenteRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UtenteNonTrovatoException(
                        "Utente con keycloakId " + keycloakId + " non trovato"
                ));
    }

    // metodo legacy, tenuto per compatibilità interna
    public Utente salvaUtente(Utente utente) {
        return utenteRepository.save(utente);
    }
}