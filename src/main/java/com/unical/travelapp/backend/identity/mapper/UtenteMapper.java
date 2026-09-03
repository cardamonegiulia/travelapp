package com.unical.travelapp.backend.identity.mapper;

import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.dto.UtenteUpdateDto;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.entity.Tema;
import org.springframework.stereotype.Component;

@Component
public class UtenteMapper {

    private final ImmagineMapper immagineMapper;

    public UtenteMapper(ImmagineMapper immagineMapper) {
        this.immagineMapper = immagineMapper;
    }

    public UtenteResponseDto toResponseDto(Utente utente) {
        UtenteResponseDto dto = new UtenteResponseDto();
        dto.setId(utente.getId());
        dto.setNome(utente.getNome());
        dto.setCognome(utente.getCognome());
        dto.setEmail(utente.getEmail());
        dto.setRuolo(utente.getRuolo());
        dto.setTema(utente.getTema());
        dto.setFotoProfilo(immagineMapper.toResponse(utente.getFotoProfilo()));
        return dto;
    }

    public Utente toEntity(UtenteDto dto) {
        Utente utente = new Utente();
        utente.setKeycloakId(dto.getKeycloakId());
        utente.setNome(dto.getNome());
        utente.setCognome(dto.getCognome());
        utente.setEmail(dto.getEmail());
        utente.setRuolo(dto.getRuolo() != null ? dto.getRuolo() : Ruolo.VIAGGIATORE);
        utente.setTema(Tema.CHIARO);
        return utente;
    }
    public void updateEntity(Utente utente, UtenteUpdateDto dto) {
        if (dto.getNome() != null) utente.setNome(dto.getNome());
        if (dto.getCognome() != null) utente.setCognome(dto.getCognome());
        if (dto.getEmail() != null) utente.setEmail(dto.getEmail());
        if(dto.getTema() != null) utente.setTema(dto.getTema());

    }
}