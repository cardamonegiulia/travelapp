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
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@AllArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final UtenteService utenteService;

    @Transactional
    public Pagamento pagaPrenotazione(Long prenotazioneId) {

        // Recupero la prenotazione controllando anche che appartenga
        // all'utente autenticato. L'admin può invece accedere a tutte.
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

        // Recupero il pagamento associato alla prenotazione.
        Pagamento pagamento = pagamentoRepository
                .findByPrenotazioneId(prenotazioneId)
                .orElseThrow(() ->
                        new PagamentoNonTrovatoException(
                                "Pagamento non trovato: " + prenotazioneId
                        )
                );

        // Una prenotazione cancellata non può essere pagata.
        if (prenotazione.getStato() == StatoPrenotazione.CANCELLATA) {
            throw new StatoPrenotazioneNonValidoException(
                    "Non puoi pagare una prenotazione cancellata: "
                            + prenotazioneId
            );
        }

        // Il pagamento può essere completato solo se è ancora in attesa.
        if (pagamento.getStato() != StatoPagamento.IN_ATTESA) {
            throw new StatoPagamentoNonValidoException(
                    "Il pagamento non può essere completato dallo stato "
                            + pagamento.getStato()
            );
        }

        // Completo il pagamento.
        pagamento.setStato(StatoPagamento.COMPLETATO);
        pagamento.setDataPagamento(LocalDateTime.now());

        // Il pagamento completato conferma anche la prenotazione.
        prenotazione.setStato(StatoPrenotazione.CONFERMATA);

        prenotazioneRepository.save(prenotazione);

        // Il service del pagamento restituisce il pagamento aggiornato.
        return pagamentoRepository.save(pagamento);
    }

    public Page<Pagamento> getPagamentiUtente(Pageable pageable) {

        Long utenteId = utenteService
                .getUtenteSessione()
                .getId();

        return pagamentoRepository
                .findByPrenotazioneViaggiatoreId(utenteId, pageable);
    }
}