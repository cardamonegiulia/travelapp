package com.unical.travelapp.backend.booking.mapper;

import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.service.PagamentoService;
import com.unical.travelapp.backend.booking.service.PrenotazioneService;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class PrenotazioneAssembler {

    private final PrenotazioneMapper mapper;
    private final PagamentoService pagamentoService;
    private final RecensioneService recensioneService;

    public PrenotazioneAssembler(
            PrenotazioneMapper mapper,
            PagamentoService pagamentoService,
            RecensioneService recensioneService) {

        this.mapper = mapper;
        this.pagamentoService = pagamentoService;
        this.recensioneService = recensioneService;
    }

    public Page<PrenotazioneResponseDto> assembla(
            Page<Prenotazione> prenotazioni) {

        List<Long> ids =
                prenotazioni
                        .getContent()
                        .stream()
                        .map(Prenotazione::getId)
                        .toList();

        Map<Long, Pagamento> pagamenti =
                pagamentoService
                        .getPagamentiPerPrenotazioni(ids);

        Map<Long, Long> recensioni =
                recensioneService
                        .getRecensioniPerPrenotazioni(ids);

        return prenotazioni.map(prenotazione -> {

            Pagamento pagamento =
                    pagamenti.get(prenotazione.getId());

            Long recensioneId =
                    recensioni.get(prenotazione.getId());

            PrenotazioneResponseDto dto =
                    mapper.toResponseDto(
                            prenotazione,
                            pagamento,
                            recensioneId
                    );

            dto.setSecondiRimanentiPagamento(
                    calcolaSecondiRimanentiPagamento(
                            prenotazione,
                            pagamento
                    )
            );

            return dto;
        });
    }

    public PrenotazioneResponseDto assembla(
            Prenotazione prenotazione) {

        Pagamento pagamento =
                pagamentoService
                        .getPagamentoPrenotazione(
                                prenotazione.getId()
                        );

        Long recensioneId =
                recensioneService
                        .getRecensioniPerPrenotazioni(
                                List.of(prenotazione.getId())
                        )
                        .get(prenotazione.getId());

        PrenotazioneResponseDto dto =
                mapper.toResponseDto(
                        prenotazione,
                        pagamento,
                        recensioneId
                );

        dto.setSecondiRimanentiPagamento(
                calcolaSecondiRimanentiPagamento(
                        prenotazione,
                        pagamento
                )
        );

        return dto;
    }

    private long calcolaSecondiRimanentiPagamento(
            Prenotazione prenotazione,
            Pagamento pagamento) {

        if (prenotazione == null ||
                pagamento == null ||
                prenotazione.getStato() != StatoPrenotazione.IN_ATTESA ||
                pagamento.getStato() != StatoPagamento.IN_ATTESA ||
                prenotazione.getDataPrenotazione() == null) {

            return 0L;
        }

        LocalDateTime scadenza =
                prenotazione
                        .getDataPrenotazione()
                        .plusMinutes(
                                PrenotazioneService
                                        .MINUTI_SCADENZA_PAGAMENTO
                        );

        long secondi =
                Duration
                        .between(
                                LocalDateTime.now(),
                                scadenza
                        )
                        .getSeconds();

        return Math.max(secondi, 0L);
    }
}