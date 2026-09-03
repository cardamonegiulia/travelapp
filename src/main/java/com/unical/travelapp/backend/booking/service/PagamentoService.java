package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.exception.PagamentoNonTrovatoException;
import com.unical.travelapp.backend.booking.exception.PrenotazioneNonTrovataException;
import com.unical.travelapp.backend.booking.exception.StatoPagamentoNonValidoException;
import com.unical.travelapp.backend.booking.exception.StatoPrenotazioneNonValidoException;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.identity.service.UtenteService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@AllArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final UtenteService utenteService;

    public void creaPagamento(
            Prenotazione prenotazione,
            BigDecimal prezzoTotale) {

        Pagamento pagamento = Pagamento.builder()
                .prenotazione(prenotazione)
                .importo(prezzoTotale)
                .stato(StatoPagamento.IN_ATTESA)
                .build();

        pagamentoRepository.save(pagamento);
    }

    @Transactional
    public Pagamento pagaPrenotazione(Long prenotazioneId) {
        Prenotazione prenotazione;

        if (utenteService.isAdmin()) {

            prenotazione = prenotazioneRepository
                    .findById(prenotazioneId)
                    .orElseThrow(() ->
                            new PrenotazioneNonTrovataException(
                                    "Prenotazione non trovata: " + prenotazioneId
                            )
                    );

        } else {

            Long utenteId = utenteService
                    .getUtenteSessione()
                    .getId();

            prenotazione = prenotazioneRepository
                    .findByIdAndViaggiatoreId(prenotazioneId, utenteId)
                    .orElseThrow(() ->
                            new PrenotazioneNonTrovataException(
                                    "Prenotazione non trovata: " + prenotazioneId
                            )
                    );
        }

        Pagamento pagamento = pagamentoRepository
                .findByPrenotazioneId(prenotazioneId)
                .orElseThrow(() ->
                        new PagamentoNonTrovatoException(
                                "Pagamento non trovato: " + prenotazioneId
                        )
                );

        if (prenotazione.getStato() == StatoPrenotazione.CANCELLATA) {
            throw new StatoPrenotazioneNonValidoException(
                    "Non puoi pagare una prenotazione cancellata: "
                            + prenotazioneId
            );
        }

        if (pagamento.getStato() != StatoPagamento.IN_ATTESA) {
            throw new StatoPagamentoNonValidoException(
                    "Il pagamento non può essere completato dallo stato "
                            + pagamento.getStato()
            );
        }
        pagamento.setStato(StatoPagamento.COMPLETATO);
        pagamento.setDataPagamento(LocalDateTime.now());

        prenotazione.setStato(StatoPrenotazione.CONFERMATA);

        prenotazioneRepository.save(prenotazione);

        return pagamentoRepository.save(pagamento);
    }

    public Page<Pagamento> getPagamentiUtente(Pageable pageable) {

        Long utenteId = utenteService
                .getUtenteSessione()
                .getId();

        return pagamentoRepository
                .findByPrenotazioneViaggiatoreId(utenteId, pageable);
    }

    @Transactional
    public Pagamento gestisciPagamentoAnnullamento(Long prenotazioneId) {

        Pagamento pagamento = pagamentoRepository
                .findByPrenotazioneId(prenotazioneId)
                .orElseThrow(() ->
                        new PagamentoNonTrovatoException(
                                "Pagamento non trovato: " + prenotazioneId
                        )
                );

        if (pagamento.getStato() == StatoPagamento.COMPLETATO) {

            pagamento.setStato(StatoPagamento.RIMBORSATO);

        } else if (pagamento.getStato() == StatoPagamento.IN_ATTESA) {

            pagamento.setStato(StatoPagamento.ANNULLATO);

        } else {
            throw new StatoPagamentoNonValidoException(
                    "Il pagamento non può essere modificato dallo stato "
                            + pagamento.getStato()
            );
        }
        return pagamentoRepository.save(pagamento);
    }

    public Pagamento getPagamentoPrenotazione(Long prenotazioneId) {
        return pagamentoRepository
                .findByPrenotazioneId(prenotazioneId)
                .orElse(null);
    }

    public Map<Long, Pagamento> getPagamentiPerPrenotazioni(List<Long> prenotazioneIds) {
        if (prenotazioneIds == null || prenotazioneIds.isEmpty()) {
            return Map.of();
        }
        return pagamentoRepository
                .findByPrenotazioneIdIn(prenotazioneIds)
                .stream()
                .collect(Collectors.toMap(
                        pagamento -> pagamento.getPrenotazione().getId(),
                        pagamento -> pagamento
                ));
    }

}