package com.unical.travelapp.backend.catalog.mapper;
import com.unical.travelapp.backend.catalog.dto.SessioneSingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaRequestDTO;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class SingolaAttivitaMapper {
    private final ImmagineMapper immagineMapper;
    public SingolaAttivitaMapper(ImmagineMapper immagineMapper) {
        this.immagineMapper = immagineMapper;
    }
    public SingolaAttivita fromRequest(SingolaAttivitaRequestDTO dto) {
        if (dto == null) return null;
        SingolaAttivita attivita = new SingolaAttivita();
        attivita.setTitolo(dto.getTitolo());
        attivita.setDescrizione(dto.getDescrizione());
        attivita.setLuogo(dto.getLuogo());
        attivita.setPrezzo(dto.getPrezzo());
        attivita.setDurataMinuti(dto.getDurataMinuti());
        attivita.setMaxPartecipanti(dto.getMaxPartecipanti());
        return attivita;
    }
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
        dto.setImmagini(
                immagineMapper.toResponse(
                        attivita.getImmagini()
                )
        );
        if (attivita.getOrganizzatore() != null) {
            dto.setOrganizzatoreId(
                    attivita.getOrganizzatore().getId()
            );
        }
        return dto;
    }
    public SessioneSingolaAttivitaDTO toSessioneDTO(
            SessioneSingolaAttivita sessione
    ) {
        if (sessione == null) {
            return null;
        }
        SessioneSingolaAttivitaDTO dto =
                new SessioneSingolaAttivitaDTO();
        dto.setId(
                sessione.getId()
        );
        dto.setDataInizio(
                sessione.getDataInizio()
        );
        dto.setDataFine(
                sessione.getDataFine()
        );
        dto.setPostiDisponibili(
                sessione.getPostiDisponibili()
        );
        dto.setStato(
                sessione.getStato()
        );
        return dto;
    }
    public List<SessioneSingolaAttivitaDTO> toSessioneDTO(
            List<SessioneSingolaAttivita> sessioni
    ) {
        if (sessioni == null) {
            return List.of();
        }
        return sessioni
                .stream()
                .map(this::toSessioneDTO)
                .toList();
    }
}
