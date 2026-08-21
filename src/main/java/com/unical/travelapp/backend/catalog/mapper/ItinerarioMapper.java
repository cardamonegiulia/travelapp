package com.unical.travelapp.backend.catalog.mapper;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import org.springframework.stereotype.Component;

@Component
public class ItinerarioMapper {

    private final ImmagineMapper immagineMapper;

    public ItinerarioMapper(ImmagineMapper immagineMapper) {
        this.immagineMapper = immagineMapper;
    }

    // id/organizzatore/stato non sono nel DTO di request: li imposta il chiamante (controller)
    public Itinerario fromRequest(ItinerarioRequestDTO dto) {
        if (dto == null) return null;

        Itinerario itinerario = new Itinerario();
        itinerario.setTitolo(dto.getTitolo());
        itinerario.setDescrizione(dto.getDescrizione());
        itinerario.setDestinazionePrincipale(dto.getDestinazionePrincipale());
        itinerario.setPrezzoBase(dto.getPrezzoBase());
        itinerario.setDurataGiorni(dto.getDurataGiorni());
        itinerario.setMaxPartecipanti(dto.getMaxPartecipanti());
        return itinerario;
    }

    public ItinerarioDTO toDTO(Itinerario itinerario) {
        if (itinerario == null) return null;

        ItinerarioDTO dto = new ItinerarioDTO();
        dto.setId(itinerario.getId());
        dto.setTitolo(itinerario.getTitolo());
        dto.setDescrizione(itinerario.getDescrizione());
        dto.setDestinazionePrincipale(itinerario.getDestinazionePrincipale());
        dto.setPrezzoBase(itinerario.getPrezzoBase());
        dto.setDurataGiorni(itinerario.getDurataGiorni());
        dto.setMaxPartecipanti(itinerario.getMaxPartecipanti());
        dto.setStato(itinerario.getStato());
        dto.setImmagini(immagineMapper.toResponse(itinerario.getImmagini()));

        if (itinerario.getOrganizzatore() != null) {
            dto.setOrganizzatoreId(itinerario.getOrganizzatore().getId());
        }
        return dto;
    }
}