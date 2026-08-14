package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.exception.StatoPagamentoNonValidoException;
import com.unical.travelapp.backend.booking.exception.StatoPrenotazioneNonValidoException;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private PrenotazioneRepository prenotazioneRepository;

    @Mock
    private UtenteService utenteService;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Utente utente() {
        Utente utente = new Utente();
        utente.setId(1L);
        return utente;
    }

    private Prenotazione prenotazione(StatoPrenotazione stato) {
        return Prenotazione.builder()
                .id(10L)
                .viaggiatore(utente())
                .numeroPartecipanti(1)
                .prezzoTotale(new BigDecimal("100.00"))
                .stato(stato)
                .dataPrenotazione(LocalDateTime.now())
                .build();
    }

    private Pagamento pagamento(
            Prenotazione prenotazione,
            StatoPagamento stato) {

        return Pagamento.builder()
                .id(20L)
                .prenotazione(prenotazione)
                .importo(new BigDecimal("100.00"))
                .stato(stato)
                .dataPagamento(LocalDateTime.now())
                .build();
    }

    @Test
    void pagamentoInAttesaVieneCompletato() {

        Prenotazione prenotazione =
                prenotazione(StatoPrenotazione.IN_ATTESA);

        Pagamento pagamento =
                pagamento(prenotazione, StatoPagamento.IN_ATTESA);

        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente());

        when(prenotazioneRepository
                .findByIdAndViaggiatoreId(10L, 1L))
                .thenReturn(Optional.of(prenotazione));

        when(pagamentoRepository.findByPrenotazioneId(10L))
                .thenReturn(Optional.of(pagamento));

        when(pagamentoRepository.save(pagamento))
                .thenReturn(pagamento);

        Pagamento risultato =
                pagamentoService.pagaPrenotazione(10L);

        assertEquals(
                StatoPagamento.COMPLETATO,
                risultato.getStato()
        );

        assertEquals(
                StatoPrenotazione.CONFERMATA,
                prenotazione.getStato()
        );

        verify(prenotazioneRepository)
                .save(prenotazione);

        verify(pagamentoRepository)
                .save(pagamento);
    }

    @Test
    void pagamentoGiaCompletatoVieneRifiutato() {

        Prenotazione prenotazione =
                prenotazione(StatoPrenotazione.IN_ATTESA);

        Pagamento pagamento =
                pagamento(prenotazione, StatoPagamento.COMPLETATO);

        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente());

        when(prenotazioneRepository
                .findByIdAndViaggiatoreId(10L, 1L))
                .thenReturn(Optional.of(prenotazione));

        when(pagamentoRepository.findByPrenotazioneId(10L))
                .thenReturn(Optional.of(pagamento));

        assertThrows(
                StatoPagamentoNonValidoException.class,
                () -> pagamentoService.pagaPrenotazione(10L)
        );

        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void pagamentoFallitoVieneRifiutato() {

        Prenotazione prenotazione =
                prenotazione(StatoPrenotazione.IN_ATTESA);

        Pagamento pagamento =
                pagamento(prenotazione, StatoPagamento.FALLITO);

        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente());

        when(prenotazioneRepository
                .findByIdAndViaggiatoreId(10L, 1L))
                .thenReturn(Optional.of(prenotazione));

        when(pagamentoRepository.findByPrenotazioneId(10L))
                .thenReturn(Optional.of(pagamento));

        assertThrows(
                StatoPagamentoNonValidoException.class,
                () -> pagamentoService.pagaPrenotazione(10L)
        );
    }

    @Test
    void prenotazioneCancellataNonPuoEsserePagata() {

        Prenotazione prenotazione =
                prenotazione(StatoPrenotazione.CANCELLATA);

        Pagamento pagamento =
                pagamento(prenotazione, StatoPagamento.IN_ATTESA);

        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente());

        when(prenotazioneRepository
                .findByIdAndViaggiatoreId(10L, 1L))
                .thenReturn(Optional.of(prenotazione));

        when(pagamentoRepository.findByPrenotazioneId(10L))
                .thenReturn(Optional.of(pagamento));

        assertThrows(
                StatoPrenotazioneNonValidoException.class,
                () -> pagamentoService.pagaPrenotazione(10L)
        );
    }

    @Test
    void pagamentoCompletatoDiventaRimborsatoConAnnullamento() {

        Prenotazione prenotazione =
                prenotazione(StatoPrenotazione.CONFERMATA);

        Pagamento pagamento =
                pagamento(prenotazione, StatoPagamento.COMPLETATO);

        when(pagamentoRepository.findByPrenotazioneId(10L))
                .thenReturn(Optional.of(pagamento));

        when(pagamentoRepository.save(pagamento))
                .thenReturn(pagamento);

        Pagamento risultato =
                pagamentoService.gestisciPagamentoAnnullamento(10L);

        assertEquals(
                StatoPagamento.RIMBORSATO,
                risultato.getStato()
        );

        verify(pagamentoRepository).save(pagamento);
    }

    @Test
    void pagamentoInAttesaDiventaFallitoConAnnullamento() {

        Prenotazione prenotazione =
                prenotazione(StatoPrenotazione.IN_ATTESA);

        Pagamento pagamento =
                pagamento(prenotazione, StatoPagamento.IN_ATTESA);

        when(pagamentoRepository.findByPrenotazioneId(10L))
                .thenReturn(Optional.of(pagamento));

        when(pagamentoRepository.save(pagamento))
                .thenReturn(pagamento);

        Pagamento risultato =
                pagamentoService.gestisciPagamentoAnnullamento(10L);

        assertEquals(
                StatoPagamento.FALLITO,
                risultato.getStato()
        );

        verify(pagamentoRepository).save(pagamento);
    }

}