package com.unical.travelapp.backend.catalog.mapper;

import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import org.springframework.stereotype.Component;

@Component
public class SingolaAttivitaMapper {

    public SingolaAttivitaDTO toDTO(SingolaAttivita attivita) {
        if (attivita == null) return null;

        SingolaAttivitaDTO dto = new SingolaAttivitaDTO();
        dto.setId(attivita.getId());
        dto.setTitolo(attivita.getTitolo());
        dto.setDescrizione(attivita.getDescrizione());
        dto.setLuogo(attivita.getLuogo());
        dto.setPrezzo(attivita.getPrezzo());
        dto.setDurataMinuti(attivita.getDurataMinuti());
        dto.setMaxPartecipanti(attivita.getMaxPartecipanti());

        if (attivita.getOrganizzatore() != null) {
            dto.setOrganizzatoreId(attivita.getOrganizzatore().getId());
        }
        return dto;
    }

    public SingolaAttivita toEntity(SingolaAttivitaDTO dto) {
        if (dto == null) return null;

        SingolaAttivita attivita = new SingolaAttivita();
        attivita.setId(dto.getId());
        attivita.setTitolo(dto.getTitolo());
        attivita.setDescrizione(dto.getDescrizione());
        attivita.setLuogo(dto.getLuogo());
        attivita.setPrezzo(dto.getPrezzo());
        attivita.setDurataMinuti(dto.getDurataMinuti());
        attivita.setMaxPartecipanti(dto.getMaxPartecipanti());
        return attivita;
    }
}