package com.unical.travelapp.backend.booking.mapper;

import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.TipoPrenotazione;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class PrenotazioneMapper {

    public PrenotazioneResponseDto toResponseDto(
            Prenotazione prenotazione,
            Pagamento pagamento
    ) {
        return toResponseDto(
                prenotazione,
                pagamento,
                null
        );
    }

    public PrenotazioneResponseDto toResponseDto(
            Prenotazione prenotazione,
            Pagamento pagamento,
            Long recensioneId
    ) {

        TipoPrenotazione tipoPrenotazione;
        String titolo;
        String luogo;

        Long itinerarioId = null;

        if (
                prenotazione.getDisponibilitaItinerario()
                        != null
        ) {

            Itinerario itinerario =
                    prenotazione
                            .getDisponibilitaItinerario()
                            .getItinerario();

            tipoPrenotazione =
                    TipoPrenotazione.ITINERARIO;

            titolo =
                    itinerario.getTitolo();

            luogo =
                    itinerario
                            .getDestinazionePrincipale();

            itinerarioId =
                    itinerario.getId();
        }

        else if (
                prenotazione
                        .getSessioneSingolaAttivita()
                        != null
        ) {

            tipoPrenotazione =
                    TipoPrenotazione.SESSIONE_SINGOLA;

            titolo =
                    prenotazione
                            .getSessioneSingolaAttivita()
                            .getSingolaAttivita()
                            .getTitolo();

            luogo =
                    prenotazione
                            .getSessioneSingolaAttivita()
                            .getSingolaAttivita()
                            .getLuogo();
        }

        else {

            throw new IllegalStateException(
                    "Prenotazione senza itinerario o sessione singola: "
                            + prenotazione.getId()
            );
        }

        boolean conclusa =
                RecensioneService.viaggioConcluso(
                        prenotazione,
                        LocalDateTime.now()
                );

        return PrenotazioneResponseDto
                .builder()

                .id(
                        prenotazione.getId()
                )

                .viaggiatoreId(
                        prenotazione
                                .getViaggiatore()
                                .getId()
                )

                .nomeViaggiatore(
                        prenotazione
                                .getViaggiatore()
                                .getNome()
                )

                .cognomeViaggiatore(
                        prenotazione
                                .getViaggiatore()
                                .getCognome()
                )

                .disponibilitaItinerarioId(
                        prenotazione
                                .getDisponibilitaItinerario()
                                != null
                                ? prenotazione
                                .getDisponibilitaItinerario()
                                .getId()
                                : null
                )

                .sessioneSingolaAttivitaId(
                        prenotazione
                                .getSessioneSingolaAttivita()
                                != null
                                ? prenotazione
                                .getSessioneSingolaAttivita()
                                .getId()
                                : null
                )

                .numeroPartecipanti(
                        prenotazione
                                .getNumeroPartecipanti()
                )

                .prezzoTotale(
                        prenotazione
                                .getPrezzoTotale()
                )

                .statoPrenotazione(
                        prenotazione
                                .getStato()
                )

                .statoPagamento(
                        pagamento != null
                                ? pagamento.getStato()
                                : null
                )

                .dataPrenotazione(
                        prenotazione
                                .getDataPrenotazione()
                )

                .tipoPrenotazione(
                        tipoPrenotazione
                )

                .titolo(
                        titolo
                )

                .luogo(
                        luogo
                )

                .destinazione(
                        luogo
                )

                .itinerarioId(
                        itinerarioId
                )

                .dataInizioViaggio(
                        dataInizio(
                                prenotazione
                        )
                )

                .dataFineViaggio(
                        RecensioneService
                                .dataFine(
                                        prenotazione
                                )
                )

                .conclusa(
                        conclusa
                )

                .recensibile(
                        conclusa &&
                                itinerarioId != null &&
                                recensioneId == null
                )

                .recensioneId(
                        recensioneId
                )

                .build();
    }

    public Page<PrenotazioneResponseDto> toResponseDtoPage(
            Page<Prenotazione> prenotazioni,
            Map<Long, Pagamento> pagamenti
    ) {

        return toResponseDtoPage(
                prenotazioni,
                pagamenti,
                Map.of()
        );
    }

    public Page<PrenotazioneResponseDto> toResponseDtoPage(
            Page<Prenotazione> prenotazioni,
            Map<Long, Pagamento> pagamenti,
            Map<Long, Long> recensioni
    ) {

        return prenotazioni.map(
                prenotazione ->
                        toResponseDto(
                                prenotazione,
                                pagamenti.get(
                                        prenotazione.getId()
                                ),
                                recensioni.get(
                                        prenotazione.getId()
                                )
                        )
        );
    }

    private LocalDateTime dataInizio(
            Prenotazione prenotazione
    ) {

        if (
                prenotazione
                        .getDisponibilitaItinerario()
                        != null
        ) {

            return prenotazione
                    .getDisponibilitaItinerario()
                    .getDataInizio();
        }

        if (
                prenotazione
                        .getSessioneSingolaAttivita()
                        != null
        ) {

            return prenotazione
                    .getSessioneSingolaAttivita()
                    .getDataInizio();
        }

        return null;
    }
}