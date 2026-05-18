package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import com.unical.travelapp.backend.identity.mapper.UtenteMapper;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtenteService {

    private final UtenteRepository utenteRepository;
    private final UtenteMapper utenteMapper;

    public UtenteService(UtenteRepository utenteRepository, UtenteMapper utenteMapper) {
        this.utenteRepository = utenteRepository;
        this.utenteMapper = utenteMapper;
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
        return utenteMapper.toResponseDto(
                utenteRepository.save(utenteMapper.toEntity(dto))
        );
    }

    public List<UtenteResponseDto> ottieniTutti() {
        return utenteRepository.findAll()
                .stream()
                .map(utenteMapper::toResponseDto)
                .toList();
    }

    public UtenteResponseDto ottieniPerId(Long id) {
        return utenteMapper.toResponseDto(
                utenteRepository.findById(id)
                        .orElseThrow(() -> new UtenteNonTrovatoException(
                                "Utente con id " + id + " non trovato"
                        ))
        );
    }

    public UtenteResponseDto ottieniPerKeycloakId(String keycloakId) {
        return utenteMapper.toResponseDto(
                utenteRepository.findByKeycloakId(keycloakId)
                        .orElseThrow(() -> new UtenteNonTrovatoException(
                                "Utente con keycloakId " + keycloakId + " non trovato"
                        ))
        );
    }

    public Utente salvaUtente(Utente utente) {
        return utenteRepository.save(utente);
    }
}