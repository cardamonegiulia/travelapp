package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.dto.PartenzaOrganizzatoreDto;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// La vista con cui l'organizzatore controlla chi ha comprato le sue partenze: mostra solo
// le partenze non ancora concluse e solo a chi ha creato l'itinerario.
@ExtendWith(MockitoExtension.class)
class PartenzeOrganizzatoreTest {

    @Mock private PrenotazioneRepository prenotazioneRepo;
    @Mock private ExtraPrenotazioneRepository extraPrenotazioneRepo;
    @Mock private AttivitaRepository attivitaRepo;
    @Mock private UtenteRepository utenteRepository;
    @Mock private DisponibilitaItinerarioRepository disponibilitaItinerarioRepository;
    @Mock private ItinerarioRepository itinerarioRepository;
    @Mock private SessioneSingolaAttivitaRepository sessioneSingolaAttivitaRepository;
    @Mock private UtenteService utenteService;
    @Mock private PagamentoService pagamentoService;

    private static final Long ITINERARIO_ID = 7L;

    private PrenotazioneService service() {
        return new PrenotazioneService(prenotazioneRepo, extraPrenotazioneRepo, attivitaRepo,
                utenteRepository, disponibilitaItinerarioRepository, itinerarioRepository,
                sessioneSingolaAttivitaRepository, utenteService, pagamentoService);
    }

    private Utente utente(Long id) {
        Utente u = new Utente();
        u.setId(id);
        return u;
    }

    private Itinerario itinerario() {
        Itinerario i = new Itinerario();
        i.setId(ITINERARIO_ID);
        return i;
    }

    private DisponibilitaItinerario disponibilita(Long id, LocalDateTime inizio, LocalDateTime fine) {
        DisponibilitaItinerario d = new DisponibilitaItinerario();
        d.setId(id);
        d.setItinerario(itinerario());
        d.setDataInizio(inizio);
        d.setDataFine(fine);
        d.setPostiDisponibili(5);
        return d;
    }

    /** L'organizzatore e' il proprietario: la query filtrata per organizzatore lo trova. */
    private void proprietarioDellItinerario() {
        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente(1L));
        when(itinerarioRepository.findByIdAndOrganizzatore_Id(ITINERARIO_ID, 1L))
                .thenReturn(Optional.of(itinerario()));
    }

    @Test
    void lePartenzeGiaConcluseNonVengonoRestituite() {
        proprietarioDellItinerario();

        LocalDateTime adesso = LocalDateTime.now();
        when(disponibilitaItinerarioRepository.findByItinerario_Id(ITINERARIO_ID)).thenReturn(List.of(
                disponibilita(1L, adesso.minusDays(30), adesso.minusDays(20)),
                disponibilita(2L, adesso.plusDays(10), adesso.plusDays(17))));
        when(prenotazioneRepo.contaPerDisponibilita(anyList(), any())).thenReturn(List.of());

        List<PartenzaOrganizzatoreDto> partenze = service().getPartenzeFuture(ITINERARIO_ID);

        assertThat(partenze)
                .extracting(PartenzaOrganizzatoreDto::getDisponibilitaId)
                .containsExactly(2L);
    }

    // Chi e' partito ieri e torna domani sta ancora viaggiando: l'organizzatore deve poter
    // vedere il gruppo che ha per le mani.
    @Test
    void unaPartenzaInCorsoRestaVisibile() {
        proprietarioDellItinerario();

        LocalDateTime adesso = LocalDateTime.now();
        when(disponibilitaItinerarioRepository.findByItinerario_Id(ITINERARIO_ID)).thenReturn(List.of(
                disponibilita(3L, adesso.minusDays(1), adesso.plusDays(1))));
        when(prenotazioneRepo.contaPerDisponibilita(anyList(), any())).thenReturn(List.of());

        assertThat(service().getPartenzeFuture(ITINERARIO_ID))
                .extracting(PartenzaOrganizzatoreDto::getDisponibilitaId)
                .containsExactly(3L);
    }

    @Test
    void lePartenzeSonoOrdinateDallaPiuVicinaEPortanoIConteggi() {
        proprietarioDellItinerario();

        LocalDateTime adesso = LocalDateTime.now();
        when(disponibilitaItinerarioRepository.findByItinerario_Id(ITINERARIO_ID)).thenReturn(List.of(
                disponibilita(20L, adesso.plusDays(40), adesso.plusDays(47)),
                disponibilita(10L, adesso.plusDays(5), adesso.plusDays(12))));
        when(prenotazioneRepo.contaPerDisponibilita(anyList(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 2L, 5L}));

        List<PartenzaOrganizzatoreDto> partenze = service().getPartenzeFuture(ITINERARIO_ID);

        assertThat(partenze)
                .extracting(PartenzaOrganizzatoreDto::getDisponibilitaId)
                .containsExactly(10L, 20L);

        assertThat(partenze.get(0).getNumeroPrenotazioni()).isEqualTo(2L);
        assertThat(partenze.get(0).getPartecipantiTotali()).isEqualTo(5L);

        // Una partenza senza prenotazioni non compare fra i conteggi: vale zero, non null.
        assertThat(partenze.get(1).getNumeroPrenotazioni()).isZero();
        assertThat(partenze.get(1).getPartecipantiTotali()).isZero();
    }

    @Test
    void unOrganizzatoreNonVedeLePartenzeDiUnItinerarioAltrui() {
        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente(2L));
        when(itinerarioRepository.findByIdAndOrganizzatore_Id(ITINERARIO_ID, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getPartenzeFuture(ITINERARIO_ID))
                .isInstanceOf(ItinerarioNonTrovatoException.class);
    }

    @Test
    void unOrganizzatoreNonVedeIPrenotatiDiUnaPartenzaAltrui() {
        when(utenteService.isAdmin()).thenReturn(false);
        when(utenteService.getUtenteSessione()).thenReturn(utente(2L));
        when(disponibilitaItinerarioRepository.findById(99L))
                .thenReturn(Optional.of(disponibilita(99L, LocalDateTime.now(), LocalDateTime.now())));
        when(itinerarioRepository.findByIdAndOrganizzatore_Id(ITINERARIO_ID, 2L))
                .thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 50);
        assertThatThrownBy(() -> service().getPrenotazioniPerPartenza(99L, pageable))
                .isInstanceOf(ItinerarioNonTrovatoException.class);
    }

    @Test
    void iPrenotatiEsclusoLeCancellazioniArrivanoAlProprietario() {
        proprietarioDellItinerario();
        when(disponibilitaItinerarioRepository.findById(99L))
                .thenReturn(Optional.of(disponibilita(99L, LocalDateTime.now(), LocalDateTime.now())));

        Pageable pageable = PageRequest.of(0, 50);
        service().getPrenotazioniPerPartenza(99L, pageable);

        org.mockito.Mockito.verify(prenotazioneRepo)
                .findByDisponibilitaItinerario(99L, StatoPrenotazione.CANCELLATA, pageable);
    }
}
