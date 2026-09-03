package com.unical.travelapp.backend.catalog.mapper;
import com.unical.travelapp.backend.catalog.dto.DisponibilitaItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.GiornoProgrammaDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.GiornoProgramma;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import com.unical.travelapp.backend.experience.models.DTO.ValutazioneMediaDTO;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Component
public class ItinerarioMapper {
    private static final LocalTime FINE_GIORNATA =
            LocalTime.of(23, 59, 59);
    private final ImmagineMapper immagineMapper;
    public ItinerarioMapper(
            ImmagineMapper immagineMapper
    ) {
        this.immagineMapper = immagineMapper;
    }
    public Itinerario fromRequest(
            ItinerarioRequestDTO dto
    ) {
        if (dto == null) {
            return null;
        }
        Itinerario itinerario =
                new Itinerario();
        itinerario.setTitolo(
                dto.getTitolo()
        );
        itinerario.setDescrizione(
                dto.getDescrizione()
        );
        itinerario.setDestinazionePrincipale(
                dto.getDestinazionePrincipale()
        );
        itinerario.setPrezzoBase(
                dto.getPrezzoBase()
        );
        itinerario.setMaxPartecipanti(
                dto.getMaxPartecipanti()
        );
        Integer durataDalPeriodo =
                dto.durataDalPeriodo();
        itinerario.setDurataGiorni(
                durataDalPeriodo != null
                        ? durataDalPeriodo
                        : dto.getDurataGiorni()
        );
        itinerario.setProgramma(
                giorniDaRequest(
                        dto.getProgramma()
                )
        );
        itinerario
                .getProgramma()
                .forEach(giorno ->
                        giorno.setItinerario(
                                itinerario
                        )
                );
        if (durataDalPeriodo != null) {
            DisponibilitaItinerario periodo =
                    new DisponibilitaItinerario();
            periodo.setDataInizio(
                    dto.getDataInizio()
                            .atStartOfDay()
            );
            periodo.setDataFine(
                    dto.getDataFine()
                            .atStartOfDay()
            );
            periodo.setPostiDisponibili(
                    dto.getMaxPartecipanti()
            );
            if (
                    dto.getDataLimitePrenotazione()
                            != null
            ) {
                periodo.setDataLimitePrenotazione(
                        dto
                                .getDataLimitePrenotazione()
                                .atTime(FINE_GIORNATA)
                );
            }
            List<DisponibilitaItinerario> disponibilita =
                    new ArrayList<>();
            disponibilita.add(periodo);
            itinerario.setDisponibilita(
                    disponibilita
            );
        }
        return itinerario;
    }
    public ItinerarioDTO toDTO(
            Itinerario itinerario
    ) {
        return toDTO(
                itinerario,
                ValutazioneMediaDTO.NESSUNA
        );
    }
    public ItinerarioDTO toDTO(
            Itinerario itinerario,
            ValutazioneMediaDTO valutazione
    ) {
        if (itinerario == null) {
            return null;
        }
        ItinerarioDTO dto =
                new ItinerarioDTO();
        dto.setId(
                itinerario.getId()
        );
        dto.setTitolo(
                itinerario.getTitolo()
        );
        dto.setDescrizione(
                itinerario.getDescrizione()
        );
        dto.setDestinazionePrincipale(
                itinerario.getDestinazionePrincipale()
        );
        dto.setPrezzoBase(
                itinerario.getPrezzoBase()
        );
        dto.setDurataGiorni(
                itinerario.getDurataGiorni()
        );
        dto.setMaxPartecipanti(
                itinerario.getMaxPartecipanti()
        );
        dto.setStato(
                itinerario.getStato()
        );
        dto.setImmagini(
                immagineMapper.toResponse(
                        itinerario.getImmagini()
                )
        );
        dto.setProgramma(
                giorniDaEntity(
                        itinerario.getProgramma()
                )
        );
        ValutazioneMediaDTO media =
                valutazione == null
                        ? ValutazioneMediaDTO.NESSUNA
                        : valutazione;
        dto.setMediaVoti(
                media.media()
        );
        dto.setNumeroRecensioni(
                media.numero()
        );
        dto.setDateDisponibili(
                haPartenzePrenotabili(
                        itinerario
                )
        );
        primaDisponibilita(
                itinerario
        ).ifPresent(periodo -> {
            dto.setDataInizio(
                    periodo
                            .getDataInizio()
                            .toLocalDate()
            );
            if (
                    periodo.getDataFine()
                            != null
            ) {
                dto.setDataFine(
                        periodo
                                .getDataFine()
                                .toLocalDate()
                );
            }
            if (
                    periodo
                            .getDataLimitePrenotazione()
                            != null
            ) {
                dto.setDataLimitePrenotazione(
                        periodo
                                .getDataLimitePrenotazione()
                                .toLocalDate()
                );
            }
        });
        if (
                itinerario.getOrganizzatore()
                        != null
        ) {
            dto.setOrganizzatoreId(
                    itinerario
                            .getOrganizzatore()
                            .getId()
            );
        }
        return dto;
    }
    public DisponibilitaItinerarioDTO toDisponibilitaDTO(
            DisponibilitaItinerario periodo
    ) {
        if (periodo == null) {
            return null;
        }
        DisponibilitaItinerarioDTO dto =
                new DisponibilitaItinerarioDTO();
        dto.setId(
                periodo.getId()
        );
        dto.setDataInizio(
                periodo.getDataInizio()
        );
        dto.setDataFine(
                periodo.getDataFine()
        );
        dto.setDataLimitePrenotazione(
                periodo.getDataLimitePrenotazione()
        );
        dto.setPostiDisponibili(
                periodo.getPostiDisponibili()
        );
        return dto;
    }
    public List<DisponibilitaItinerarioDTO> toDisponibilitaDTO(
            List<DisponibilitaItinerario> periodi
    ) {
        if (periodi == null) {
            return List.of();
        }
        return periodi
                .stream()
                .map(this::toDisponibilitaDTO)
                .toList();
    }
    private List<GiornoProgramma> giorniDaRequest(
            List<GiornoProgrammaDTO> giorni
    ) {
        List<GiornoProgramma> risultato =
                new ArrayList<>();
        if (giorni == null) {
            return risultato;
        }
        int numero = 1;
        for (GiornoProgrammaDTO richiesto : giorni) {
            if (richiesto == null) {
                continue;
            }
            GiornoProgramma giorno =
                    new GiornoProgramma();
            giorno.setGiorno(numero++);
            giorno.setTitolo(
                    richiesto.getTitolo()
            );
            giorno.setDescrizione(
                    richiesto.getDescrizione()
            );
            risultato.add(giorno);
        }
        return risultato;
    }
    private List<GiornoProgrammaDTO> giorniDaEntity(
            List<GiornoProgramma> giorni
    ) {
        if (giorni == null) {
            return new ArrayList<>();
        }
        return giorni
                .stream()
                .map(giorno -> {
                    GiornoProgrammaDTO dto =
                            new GiornoProgrammaDTO();
                    dto.setGiorno(
                            giorno.getGiorno()
                    );
                    dto.setTitolo(
                            giorno.getTitolo()
                    );
                    dto.setDescrizione(
                            giorno.getDescrizione()
                    );
                    return dto;
                })
                .collect(
                        Collectors.toCollection(
                                ArrayList::new
                        )
                );
    }
    private Optional<DisponibilitaItinerario> primaDisponibilita(
            Itinerario itinerario
    ) {
        if (
                itinerario.getDisponibilita()
                        == null
        ) {
            return Optional.empty();
        }
        LocalDateTime adesso =
                LocalDateTime.now();
        return itinerario
                .getDisponibilita()
                .stream()
                .filter(
                        disponibilita ->
                                disponibilita.getDataInizio()
                                        != null
                )
                .filter(
                        disponibilita ->
                                disponibilita.prenotabileAl(
                                        adesso
                                )
                )
                .min(
                        Comparator.comparing(
                                DisponibilitaItinerario::getDataInizio
                        )
                );
    }
    private boolean haPartenzePrenotabili(
            Itinerario itinerario
    ) {
        if (
                itinerario.getDisponibilita()
                        == null
        ) {
            return false;
        }
        LocalDateTime adesso =
                LocalDateTime.now();
        return itinerario
                .getDisponibilita()
                .stream()
                .anyMatch(periodo ->
                        periodo.prenotabileAl(
                                adesso
                        )
                );
    }
}
