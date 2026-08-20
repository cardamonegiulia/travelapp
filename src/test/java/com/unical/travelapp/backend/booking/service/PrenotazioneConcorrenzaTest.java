package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.TestDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class PrenotazioneConcorrenzaTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.applica(registry);
    }

    @Test
    void deveBloccarePagamentoEAnnullamentoConcorrentiSullaStessaPrenotazione() {

        // 1. Creo una prenotazione inizialmente IN_ATTESA
        EntityManager setup = entityManagerFactory.createEntityManager();

        setup.getTransaction().begin();

        Utente viaggiatore = new Utente();
        viaggiatore.setKeycloakId("test-concorrenza-prenotazione");
        viaggiatore.setNome("Mario");
        viaggiatore.setCognome("Rossi");
        viaggiatore.setEmail("mario.rossi-concorrenza@test.it");
        viaggiatore.setRuolo(Ruolo.VIAGGIATORE);

        setup.persist(viaggiatore);

        Prenotazione prenotazione = Prenotazione.builder()
                .numeroPartecipanti(1)
                .prezzoTotale(new BigDecimal("100.00"))
                .stato(StatoPrenotazione.IN_ATTESA)
                .dataPrenotazione(LocalDateTime.now())
                .viaggiatore(viaggiatore)
                .build();

        setup.persist(prenotazione);

        setup.getTransaction().commit();

        Long prenotazioneId = prenotazione.getId();

        setup.close();


        // 2. Simulo due richieste concorrenti
        EntityManager emPagamento =
                entityManagerFactory.createEntityManager();

        EntityManager emAnnullamento =
                entityManagerFactory.createEntityManager();

        try {

            emPagamento.getTransaction().begin();
            emAnnullamento.getTransaction().begin();

            Prenotazione prenotazionePagamento =
                    emPagamento.find(
                            Prenotazione.class,
                            prenotazioneId
                    );

            Prenotazione prenotazioneAnnullamento =
                    emAnnullamento.find(
                            Prenotazione.class,
                            prenotazioneId
                    );


            // Entrambe hanno letto lo stesso stato iniziale
            assertEquals(
                    StatoPrenotazione.IN_ATTESA,
                    prenotazionePagamento.getStato()
            );

            assertEquals(
                    StatoPrenotazione.IN_ATTESA,
                    prenotazioneAnnullamento.getStato()
            );


            // 3. La prima richiesta simula il pagamento
            prenotazionePagamento.setStato(
                    StatoPrenotazione.CONFERMATA
            );


            // 4. La seconda richiesta simula l'annullamento
            prenotazioneAnnullamento.setStato(
                    StatoPrenotazione.CANCELLATA
            );


            // 5. Il pagamento vince e aggiorna la version
            emPagamento.getTransaction().commit();


            // 6. L'annullamento possiede ancora la vecchia version
            assertThrows(
                    OptimisticLockException.class,
                    emAnnullamento::flush
            );

        } finally {

            if (emAnnullamento.getTransaction().isActive()) {
                emAnnullamento.getTransaction().rollback();
            }

            if (emPagamento.getTransaction().isActive()) {
                emPagamento.getTransaction().rollback();
            }

            if (emPagamento.isOpen()) {
                emPagamento.close();
            }

            if (emAnnullamento.isOpen()) {
                emAnnullamento.close();
            }
        }


        // 7. Verifico lo stato finale
        EntityManager verifica =
                entityManagerFactory.createEntityManager();

        Prenotazione risultato =
                verifica.find(
                        Prenotazione.class,
                        prenotazioneId
                );

        assertEquals(
                StatoPrenotazione.CONFERMATA,
                risultato.getStato()
        );

        assertEquals(
                1L,
                risultato.getVersion()
        );

        verifica.close();
    }
}