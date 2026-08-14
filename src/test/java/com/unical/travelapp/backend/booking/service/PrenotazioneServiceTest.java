package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.exception.RichiestaPrenotazioneNonValidaException;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PrenotazioneServiceTest {

    @Mock
    private PrenotazioneRepository prenotazioneRepo;

    @Mock
    private ExtraPrenotazioneRepository extraPrenotazioneRepo;

    @Mock
    private PagamentoRepository pagamentoRepo;

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
                pagamentoRepo,
                attivitaRepo,
                utenteRepository,
                disponibilitaItinerarioRepository,
                sessioneSingolaAttivitaRepository,
                utenteService,
                pagamentoService
        );
    }
}