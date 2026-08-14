package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.exception.PrenotazioneNonTrovataException;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


// Verifica il fix BOLA: getPrenotazioneById/getPrenotazioniByUtente usano l'ownership
// nella query (404 se la prenotazione non e' dell'utente corrente), non un semplice findById.
@ExtendWith(MockitoExtension.class)
class PrenotazioneServiceOwnershipTest {

    @Mock private PrenotazioneRepository prenotazioneRepo;
    @Mock private ExtraPrenotazioneRepository extraPrenotazioneRepo;
    @Mock private PagamentoRepository pagamentoRepo;
    @Mock private AttivitaRepository attivitaRepo;
    @Mock private UtenteRepository utenteRepository;
    @Mock private DisponibilitaItinerarioRepository disponibilitaItinerarioRepository;
    @Mock private SessioneSingolaAttivitaRepository sessioneSingolaAttivitaRepository;
    @Mock private UtenteService utenteService;
    @Mock private PagamentoService pagamentoService;

    private PrenotazioneService service() {
        return new PrenotazioneService(prenotazioneRepo, extraPrenotazioneRepo, pagamentoRepo, attivitaRepo,
                utenteRepository, disponibilitaItinerarioRepository, sessioneSingolaAttivitaRepository, utenteService, pagamentoService);
    }

    private Utente utente(Long id) {
        Utente u = new Utente();
        u.setId(id);
        return u;
    }

    @Test
    void unUtenteNonAdminNonPuoLeggereLaPrenotazioneDiUnAltro() {
        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente(1L));
        // la prenotazione richiesta appartiene a un altro utente: la query ownership-filtrata non la trova
        when(prenotazioneRepo.findByIdAndViaggiatoreId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getPrenotazioneById(10L))
                .isInstanceOf(PrenotazioneNonTrovataException.class);
    }

    @Test
    void unUtenteNonAdminPuoLeggereLaPropriaPrenotazione() {
        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente(1L));
        Prenotazione propria = new Prenotazione();
        propria.setId(10L);
        when(prenotazioneRepo.findByIdAndViaggiatoreId(10L, 1L)).thenReturn(Optional.of(propria));

        assertThat(service().getPrenotazioneById(10L)).isSameAs(propria);
    }

    @Test
    void unAdminPuoLeggereLaPrenotazioneDiQualsiasiUtente() {
        when(utenteService.isAdmin()).thenReturn(true);
        Prenotazione altrui = new Prenotazione();
        altrui.setId(10L);
        when(prenotazioneRepo.findById(10L)).thenReturn(Optional.of(altrui));

        assertThat(service().getPrenotazioneById(10L)).isSameAs(altrui);
    }

    @Test
    void unUtenteNonPuoElencareLePrenotazioniDiUnAltroUtente() {
        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente(1L));

        Pageable pageable = PageRequest.of(0, 20);
        assertThatThrownBy(() -> service().getPrenotazioniByUtente(2L, pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

}
