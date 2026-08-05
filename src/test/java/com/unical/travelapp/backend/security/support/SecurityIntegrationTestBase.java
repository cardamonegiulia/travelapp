package com.unical.travelapp.backend.security.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.SingolaAttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.TappaRepository;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.experience.repository.PreferitoRepository;
import com.unical.travelapp.backend.experience.repository.RecensioneRepository;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Tema;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Base dei test di sicurezza end-to-end sulla catena di filtri reale.
 *
 * <p>Volutamente NON usa {@code @AutoConfigureMockMvc(addFilters = false)}: i test devono
 * attraversare l'intera SecurityFilterChain di produzione (autenticazione, autorizzazione,
 * header di sicurezza, rate limit, CORS), altrimenti non verificherebbero nulla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class SecurityIntegrationTestBase {

    protected static final String SUB_UTENTE_A = "sub-utente-a";
    protected static final String SUB_UTENTE_B = "sub-utente-b";
    protected static final String SUB_ORGANIZZATORE = "sub-organizzatore";
    protected static final String SUB_ADMIN = "sub-admin";

    @Autowired protected MockMvc mockMvc;

    /** Mapper solo per costruire i payload di test: quello dell'applicazione si collauda via HTTP. */
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired protected UtenteRepository utenteRepository;
    @Autowired protected ItinerarioRepository itinerarioRepository;
    @Autowired protected DisponibilitaItinerarioRepository disponibilitaRepository;
    @Autowired protected PrenotazioneRepository prenotazioneRepository;
    @Autowired protected PagamentoRepository pagamentoRepository;
    @Autowired protected RecensioneRepository recensioneRepository;
    @Autowired protected PreferitoRepository preferitoRepository;
    @Autowired protected ExtraPrenotazioneRepository extraPrenotazioneRepository;
    @Autowired protected SessioneSingolaAttivitaRepository sessioneRepository;
    @Autowired protected SingolaAttivitaRepository singolaAttivitaRepository;
    @Autowired protected AttivitaRepository attivitaRepository;
    @Autowired protected TappaRepository tappaRepository;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.applica(registry);
    }

    @BeforeEach
    void ripulisciIlDatabase() {
        extraPrenotazioneRepository.deleteAll();
        pagamentoRepository.deleteAll();
        recensioneRepository.deleteAll();
        preferitoRepository.deleteAll();
        prenotazioneRepository.deleteAll();
        sessioneRepository.deleteAll();
        disponibilitaRepository.deleteAll();
        attivitaRepository.deleteAll();
        tappaRepository.deleteAll();
        singolaAttivitaRepository.deleteAll();
        itinerarioRepository.deleteAll();
        utenteRepository.deleteAll();
    }

    // --- helper di seeding -------------------------------------------------

    protected Utente utente(String keycloakSub, Ruolo ruolo) {
        Utente utente = new Utente();
        utente.setKeycloakId(keycloakSub);
        utente.setNome("Nome" + keycloakSub.hashCode());
        utente.setCognome("Cognome");
        utente.setEmail(keycloakSub + "@example.test");
        utente.setRuolo(ruolo);
        utente.setTema(Tema.CHIARO);
        return utenteRepository.save(utente);
    }

    protected Itinerario itinerario(Utente organizzatore) {
        Itinerario itinerario = new Itinerario();
        itinerario.setOrganizzatore(organizzatore);
        itinerario.setTitolo("Giro della Calabria");
        itinerario.setDescrizione("Itinerario di prova");
        itinerario.setDestinazionePrincipale("Tropea");
        itinerario.setPrezzoBase(new BigDecimal("100.00"));
        itinerario.setDurataGiorni(3);
        itinerario.setMaxPartecipanti(20);
        itinerario.setStato("PUBBLICATO");
        return itinerarioRepository.save(itinerario);
    }

    protected DisponibilitaItinerario disponibilita(Itinerario itinerario, int posti) {
        DisponibilitaItinerario disponibilita = new DisponibilitaItinerario();
        disponibilita.setItinerario(itinerario);
        disponibilita.setDataInizio(LocalDateTime.now().plusDays(30));
        disponibilita.setDataFine(LocalDateTime.now().plusDays(33));
        disponibilita.setPostiDisponibili(posti);
        return disponibilitaRepository.save(disponibilita);
    }

    protected Prenotazione prenotazione(Utente viaggiatore, DisponibilitaItinerario disponibilita) {
        Prenotazione prenotazione = Prenotazione.builder()
                .viaggiatore(viaggiatore)
                .disponibilitaItinerario(disponibilita)
                .numeroPartecipanti(2)
                .prezzoTotale(new BigDecimal("200.00"))
                .stato(StatoPrenotazione.IN_ATTESA)
                .dataPrenotazione(LocalDateTime.now())
                .build();
        Prenotazione salvata = prenotazioneRepository.save(prenotazione);

        pagamentoRepository.save(Pagamento.builder()
                .prenotazione(salvata)
                .importo(salvata.getPrezzoTotale())
                .dataPagamento(LocalDateTime.now())
                .stato(StatoPagamento.IN_ATTESA)
                .build());

        return salvata;
    }

    protected Recensione recensione(Utente autore, Itinerario itinerario) {
        Recensione recensione = new Recensione();
        recensione.setUtente(autore);
        recensione.setItinerario(itinerario);
        recensione.setVoto(4);
        recensione.setCommento("Bel viaggio");
        return recensioneRepository.save(recensione);
    }

    protected SessioneSingolaAttivita sessione(Utente organizzatore, int posti) {
        SingolaAttivita attivita = new SingolaAttivita();
        attivita.setOrganizzatore(organizzatore);
        attivita.setTitolo("Escursione");
        attivita.setLuogo("Sila");
        attivita.setPrezzo(new BigDecimal("30.00"));
        attivita.setMaxPartecipanti(posti);
        SingolaAttivita salvata = singolaAttivitaRepository.save(attivita);

        SessioneSingolaAttivita sessione = new SessioneSingolaAttivita();
        sessione.setSingolaAttivita(salvata);
        sessione.setDataInizio(LocalDateTime.now().plusDays(10));
        sessione.setDataFine(LocalDateTime.now().plusDays(10).plusHours(4));
        sessione.setPostiDisponibili(posti);
        sessione.setStato("ATTIVA");
        return sessioneRepository.save(sessione);
    }
}
