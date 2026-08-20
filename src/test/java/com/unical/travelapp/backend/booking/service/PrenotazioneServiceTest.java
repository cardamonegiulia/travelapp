package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.exception.PostiInsufficientiException;
import com.unical.travelapp.backend.booking.exception.RichiestaPrenotazioneNonValidaException;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrenotazioneServiceTest {
    @Mock
    private PrenotazioneRepository prenotazioneRepo;

    @Mock
    private ExtraPrenotazioneRepository extraPrenotazioneRepo;

    @Mock
    private AttivitaRepository attivitaRepo;

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private DisponibilitaItinerarioRepository disponibilitaItinerarioRepository;

    @Mock
    private SessioneSingolaAttivitaRepository sessioneSingolaAttivitaRepository;

    @Mock
    private UtenteService utenteService;

    @Mock
    private PagamentoService pagamentoService;

    @InjectMocks
    private PrenotazioneService prenotazioneService;

    @Test
    void rifiutaAttivitaExtraDuplicate() {

        CreaPrenotazioneRequest request = new CreaPrenotazioneRequest();

        request.setDisponibilitaItinerarioId(10L);
        request.setNumeroPartecipanti(1);
        request.setAttivitaExtraIds(List.of(5L, 5L));

        assertThrows(
                RichiestaPrenotazioneNonValidaException.class,
                () -> prenotazioneService.createPrenotazione(request)
        );

        // La richiesta deve essere bloccata durante la validazione,
        // prima di leggere o salvare dati nel database.
        verifyNoInteractions(
                prenotazioneRepo,
                extraPrenotazioneRepo,
                attivitaRepo,
                utenteRepository,
                disponibilitaItinerarioRepository,
                sessioneSingolaAttivitaRepository,
                utenteService,
                pagamentoService
        );
    }

    @Test
    void rifiutaPrenotazioneItinerarioQuandoIPostiNonBastano() {

        CreaPrenotazioneRequest request = new CreaPrenotazioneRequest();
        request.setDisponibilitaItinerarioId(10L);
        request.setNumeroPartecipanti(4);

        Utente utente = new Utente();

        DisponibilitaItinerario disponibilita = new DisponibilitaItinerario();
        disponibilita.setPostiDisponibili(3);

        when(utenteService.getUtenteSessione()).thenReturn(utente);
        when(disponibilitaItinerarioRepository.findById(10L))
                .thenReturn(Optional.of(disponibilita));

        assertThrows(
                PostiInsufficientiException.class,
                () -> prenotazioneService.createPrenotazione(request)
        );

        assertEquals(3, disponibilita.getPostiDisponibili());
    }

    @Test
    void rifiutaPrenotazioneSessioneQuandoIPostiNonBastano() {

        CreaPrenotazioneRequest request = new CreaPrenotazioneRequest();
        request.setSessioneSingolaAttivitaId(20L);
        request.setNumeroPartecipanti(5);

        Utente utente = new Utente();

        SessioneSingolaAttivita sessione = new SessioneSingolaAttivita();
        sessione.setPostiDisponibili(2);

        when(utenteService.getUtenteSessione()).thenReturn(utente);
        when(sessioneSingolaAttivitaRepository.findById(20L))
                .thenReturn(Optional.of(sessione));

        assertThrows(
                PostiInsufficientiException.class,
                () -> prenotazioneService.createPrenotazione(request)
        );

        assertEquals(2, sessione.getPostiDisponibili());
    }


}