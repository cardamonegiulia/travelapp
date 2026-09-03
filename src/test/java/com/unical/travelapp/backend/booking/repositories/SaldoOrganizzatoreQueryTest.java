package com.unical.travelapp.backend.booking.repositories;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.TestDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class SaldoOrganizzatoreQueryTest {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private PagamentoRepository pagamentoRepo;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.applica(registry);
    }

    @Test
    void sommaSoloIlDenaroIncassatoSuEntrambiIRamiDelCatalogo() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Utente organizzatore = utente("saldo-org", "org-saldo@test.it", Ruolo.ORGANIZZATORE);
        Utente altroOrganizzatore = utente("saldo-altro", "altro-saldo@test.it", Ruolo.ORGANIZZATORE);
        Utente viaggiatore = utente("saldo-viagg", "viagg-saldo@test.it", Ruolo.VIAGGIATORE);
        em.persist(organizzatore);
        em.persist(altroOrganizzatore);
        em.persist(viaggiatore);

        pagata(em, prenotazione(viaggiatore, disponibilita(em, organizzatore), null,
                new BigDecimal("100.00"), StatoPrenotazione.CONFERMATA));

        pagata(em, prenotazione(viaggiatore, null, sessione(em, organizzatore),
                new BigDecimal("50.00"), StatoPrenotazione.CONFERMATA));

        rimborsata(em, prenotazione(viaggiatore, disponibilita(em, organizzatore), null,
                new BigDecimal("999.00"), StatoPrenotazione.CANCELLATA));

        annullata(em, prenotazione(viaggiatore, disponibilita(em, organizzatore), null,
                new BigDecimal("888.00"), StatoPrenotazione.CANCELLATA));

        inAttesa(em, prenotazione(viaggiatore, disponibilita(em, organizzatore), null,
                new BigDecimal("777.00"), StatoPrenotazione.IN_ATTESA));

        pagata(em, prenotazione(viaggiatore, disponibilita(em, altroOrganizzatore), null,
                new BigDecimal("666.00"), StatoPrenotazione.CONFERMATA));

        em.getTransaction().commit();
        Long organizzatoreId = organizzatore.getId();
        Long altroId = altroOrganizzatore.getId();
        em.close();

        assertEquals(0, new BigDecimal("150.00").compareTo(saldo(organizzatoreId)));
        assertEquals(0, new BigDecimal("666.00").compareTo(saldo(altroId)));
    }

    private BigDecimal saldo(Long organizzatoreId) {
        return pagamentoRepo.sumIncassatoPerOrganizzatore(
                organizzatoreId, StatoPagamento.COMPLETATO, StatoPrenotazione.CANCELLATA);
    }

    private Utente utente(String keycloakId, String email, Ruolo ruolo) {
        Utente u = new Utente();
        u.setKeycloakId(keycloakId);
        u.setNome("Test");
        u.setCognome("Saldo");
        u.setEmail(email);
        u.setRuolo(ruolo);
        return u;
    }

    private DisponibilitaItinerario disponibilita(EntityManager em, Utente organizzatore) {
        Itinerario itinerario = new Itinerario();
        itinerario.setOrganizzatore(organizzatore);
        itinerario.setTitolo("Itinerario saldo");
        itinerario.setPrezzoBase(new BigDecimal("100.00"));
        em.persist(itinerario);

        DisponibilitaItinerario disp = new DisponibilitaItinerario();
        disp.setItinerario(itinerario);
        disp.setDataInizio(LocalDateTime.now().plusDays(1));
        disp.setDataFine(LocalDateTime.now().plusDays(5));
        disp.setPostiDisponibili(10);
        em.persist(disp);
        return disp;
    }

    private SessioneSingolaAttivita sessione(EntityManager em, Utente organizzatore) {
        SingolaAttivita attivita = new SingolaAttivita();
        attivita.setOrganizzatore(organizzatore);
        attivita.setTitolo("Attivita saldo");
        attivita.setPrezzo(new BigDecimal("50.00"));
        em.persist(attivita);

        SessioneSingolaAttivita sessione = new SessioneSingolaAttivita();
        sessione.setSingolaAttivita(attivita);
        sessione.setDataInizio(LocalDateTime.now().plusDays(1));
        sessione.setDataFine(LocalDateTime.now().plusDays(1).plusHours(2));
        sessione.setPostiDisponibili(10);
        em.persist(sessione);
        return sessione;
    }

    private Prenotazione prenotazione(Utente viaggiatore,
                                      DisponibilitaItinerario disp,
                                      SessioneSingolaAttivita sessione,
                                      BigDecimal prezzo,
                                      StatoPrenotazione stato) {
        return Prenotazione.builder()
                .viaggiatore(viaggiatore)
                .disponibilitaItinerario(disp)
                .sessioneSingolaAttivita(sessione)
                .numeroPartecipanti(1)
                .prezzoTotale(prezzo)
                .stato(stato)
                .dataPrenotazione(LocalDateTime.now())
                .build();
    }

    private void pagata(EntityManager em, Prenotazione prenotazione) {
        pagamento(em, prenotazione, StatoPagamento.COMPLETATO);
    }

    private void rimborsata(EntityManager em, Prenotazione prenotazione) {
        pagamento(em, prenotazione, StatoPagamento.RIMBORSATO);
    }

    private void annullata(EntityManager em, Prenotazione prenotazione) {
        pagamento(em, prenotazione, StatoPagamento.ANNULLATO);
    }

    private void inAttesa(EntityManager em, Prenotazione prenotazione) {
        pagamento(em, prenotazione, StatoPagamento.IN_ATTESA);
    }

    private void pagamento(EntityManager em, Prenotazione prenotazione, StatoPagamento stato) {
        em.persist(prenotazione);
        em.persist(Pagamento.builder()
                .prenotazione(prenotazione)
                .importo(prenotazione.getPrezzoTotale())
                .dataPagamento(stato == StatoPagamento.IN_ATTESA ? null : LocalDateTime.now())
                .stato(stato)
                .build());
    }
}
