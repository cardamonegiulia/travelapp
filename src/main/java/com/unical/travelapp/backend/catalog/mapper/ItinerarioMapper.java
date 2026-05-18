package com.unical.travelapp.backend.catalog.mapper;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import org.springframework.stereotype.Component;

@Component
public class ItinerarioMapper {

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

        if (itinerario.getOrganizzatore() != null) {
            dto.setOrganizzatoreId(itinerario.getOrganizzatore().getId());
        }
        return dto;
    }

    public Itinerario toEntity(ItinerarioDTO dto) {
        if (dto == null) return null;

        Itinerario itinerario = new Itinerario();
        itinerario.setId(dto.getId());
        itinerario.setTitolo(dto.getTitolo());
        itinerario.setDescrizione(dto.getDescrizione());
        itinerario.setDestinazionePrincipale(dto.getDestinazionePrincipale());
        itinerario.setPrezzoBase(dto.getPrezzoBase());
        itinerario.setDurataGiorni(dto.getDurataGiorni());
        itinerario.setMaxPartecipanti(dto.getMaxPartecipanti());
        itinerario.setStato(dto.getStato());
        return itinerario;
    }
}