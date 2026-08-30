package com.unical.travelapp.backend.catalog.mapper;

import com.unical.travelapp.backend.catalog.dto.DisponibilitaItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ItinerarioMapper {

    // Ultimo istante prenotabile del giorno indicato: LocalTime.MAX (.999999999) verrebbe
    // arrotondato dal database al giorno successivo, spostando il termine di 24 ore.
    private static final LocalTime FINE_GIORNATA = LocalTime.of(23, 59, 59);

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
        itinerario.setMaxPartecipanti(dto.getMaxPartecipanti());

        // Quando il client manda il periodo la durata la decide il server: cosi' non puo'
        // esistere un itinerario con date e durata che si contraddicono.
        Integer durataDalPeriodo = dto.durataDalPeriodo();
        itinerario.setDurataGiorni(durataDalPeriodo != null ? durataDalPeriodo : dto.getDurataGiorni());

        if (durataDalPeriodo != null) {
            DisponibilitaItinerario periodo = new DisponibilitaItinerario();
            periodo.setDataInizio(dto.getDataInizio().atStartOfDay());
            periodo.setDataFine(dto.getDataFine().atStartOfDay());
            periodo.setPostiDisponibili(dto.getMaxPartecipanti());

            if (dto.getDataLimitePrenotazione() != null) {
                // Termine inclusivo: si prenota per tutto il giorno indicato.
                periodo.setDataLimitePrenotazione(dto.getDataLimitePrenotazione().atTime(FINE_GIORNATA));
            }

            List<DisponibilitaItinerario> disponibilita = new ArrayList<>();
            disponibilita.add(periodo);
            itinerario.setDisponibilita(disponibilita);
        }

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

        // Il periodo esposto e' quello della partenza piu' vicina fra le disponibilita'.
        primaDisponibilita(itinerario).ifPresent(periodo -> {
            dto.setDataInizio(periodo.getDataInizio().toLocalDate());
            if (periodo.getDataFine() != null) {
                dto.setDataFine(periodo.getDataFine().toLocalDate());
            }
            if (periodo.getDataLimitePrenotazione() != null) {
                dto.setDataLimitePrenotazione(periodo.getDataLimitePrenotazione().toLocalDate());
            }
        });

        if (itinerario.getOrganizzatore() != null) {
            dto.setOrganizzatoreId(itinerario.getOrganizzatore().getId());
        }
        return dto;
    }

    public DisponibilitaItinerarioDTO toDisponibilitaDTO(DisponibilitaItinerario periodo) {
        if (periodo == null) return null;

        DisponibilitaItinerarioDTO dto = new DisponibilitaItinerarioDTO();
        dto.setId(periodo.getId());
        dto.setDataInizio(periodo.getDataInizio());
        dto.setDataFine(periodo.getDataFine());
        dto.setDataLimitePrenotazione(periodo.getDataLimitePrenotazione());
        dto.setPostiDisponibili(periodo.getPostiDisponibili());
        return dto;
    }

    public List<DisponibilitaItinerarioDTO> toDisponibilitaDTO(List<DisponibilitaItinerario> periodi) {
        if (periodi == null) return List.of();
        return periodi.stream().map(this::toDisponibilitaDTO).toList();
    }

    private Optional<DisponibilitaItinerario> primaDisponibilita(Itinerario itinerario) {
        if (itinerario.getDisponibilita() == null) {
            return Optional.empty();
        }
        return itinerario.getDisponibilita().stream()
                .filter(d -> d.getDataInizio() != null)
                .min(Comparator.comparing(DisponibilitaItinerario::getDataInizio));
    }
}