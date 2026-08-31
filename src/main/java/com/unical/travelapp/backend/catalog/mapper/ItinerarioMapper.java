package com.unical.travelapp.backend.catalog.mapper;

import com.unical.travelapp.backend.catalog.dto.DisponibilitaItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
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

@Component
public class ItinerarioMapper {

    // Ultimo istante prenotabile del giorno indicato.
    private static final LocalTime FINE_GIORNATA =
            LocalTime.of(23, 59, 59);

    private final ImmagineMapper immagineMapper;

    public ItinerarioMapper(
            ImmagineMapper immagineMapper
    ) {
        this.immagineMapper = immagineMapper;
    }

    /*
     * ============================================================
     * REQUEST -> ENTITY
     * ============================================================
     */

    // id/organizzatore/stato non sono nel DTO di request:
    // li imposta il controller.
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

        /*
         * Se il client invia dataInizio/dataFine,
         * la durata viene calcolata dal server.
         */
        Integer durataDalPeriodo =
                dto.durataDalPeriodo();

        itinerario.setDurataGiorni(
                durataDalPeriodo != null
                        ? durataDalPeriodo
                        : dto.getDurataGiorni()
        );

        /*
         * Se è stato indicato un periodo,
         * creiamo anche la disponibilità iniziale.
         */
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

    /*
     * ============================================================
     * ENTITY -> DTO
     * ============================================================
     */

    /**
     * Vista senza valutazione.
     * Usata dove recensioni/media non servono.
     */
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

        /*
         * Valutazione media.
         */
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

        /*
         * Indica se esiste almeno una partenza
         * ancora prenotabile temporalmente.
         */
        dto.setDateDisponibili(
                haPartenzePrenotabili(
                        itinerario
                )
        );

        /*
         * Espone sul DTO principale il periodo
         * della prima disponibilità.
         */
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

    /*
     * ============================================================
     * DISPONIBILITÀ -> DTO
     * ============================================================
     */

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

    /*
     * ============================================================
     * SUPPORTO
     * ============================================================
     */

    private Optional<DisponibilitaItinerario> primaDisponibilita(
            Itinerario itinerario
    ) {

        if (
                itinerario.getDisponibilita()
                        == null
        ) {
            return Optional.empty();
        }

        return itinerario
                .getDisponibilita()
                .stream()
                .filter(
                        disponibilita ->
                                disponibilita.getDataInizio()
                                        != null
                )
                .min(
                        Comparator.comparing(
                                DisponibilitaItinerario::getDataInizio
                        )
                );
    }

    /**
     * Vero se rimane almeno una partenza il cui
     * termine di prenotazione non è ancora passato.
     *
     * I posti non vengono considerati qui:
     * "esaurito" e "nessuna data disponibile"
     * sono due condizioni differenti.
     */
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
                .anyMatch(periodo -> {

                    LocalDateTime termine =
                            periodo
                                    .getDataLimitePrenotazione()
                                    != null
                                    ? periodo
                                    .getDataLimitePrenotazione()
                                    : periodo
                                    .getDataInizio();

                    return termine != null
                            && !termine.isBefore(
                            adesso
                    );
                });
    }
}