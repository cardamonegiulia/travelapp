package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
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

    public UtenteResponseDto salvaUtenteDatoDTO(UtenteDto dto) {
        if (utenteRepository.existsByEmail(dto.getEmail())) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con email: " + dto.getEmail()
            );
        }
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

        return convertiInDto(utenteRepository.save(utente));
    }

    public List<UtenteResponseDto> ottieniTutti() {
        return utenteRepository.findAll()
                .stream()
                .map(this::convertiInDto)
                .toList();
    }

    public UtenteResponseDto ottieniPerId(Long id) {
        Utente utente = utenteRepository.findById(id)
                .orElseThrow(() -> new UtenteNonTrovatoException(
                        "Utente con id " + id + " non trovato"
                ));
        return convertiInDto(utente);
    }

    public UtenteResponseDto ottieniPerKeycloakId(String keycloakId) {
        Utente utente = utenteRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UtenteNonTrovatoException(
                        "Utente con keycloakId " + keycloakId + " non trovato"
                ));
        return convertiInDto(utente);
    }

    // metodo legacy, tenuto per compatibilità interna
    public Utente salvaUtente(Utente utente) {
        return utenteRepository.save(utente);
    }

    private UtenteResponseDto convertiInDto(Utente utente) {
        UtenteResponseDto dto = new UtenteResponseDto();
        dto.setId(utente.getId());
        dto.setNome(utente.getNome());
        dto.setCognome(utente.getCognome());
        dto.setEmail(utente.getEmail());
        dto.setRuolo(utente.getRuolo());
        return dto;
    }
}