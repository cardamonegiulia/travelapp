package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
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
class PagamentoConcorrenzaTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.applica(registry);
    }

    @Test
    void deveBloccareDuePagamentiConcorrentiSullaStessaPrenotazione() {
        // 1. Creo una prenotazione e il relativo pagamento
        EntityManager setup = entityManagerFactory.createEntityManager();

        setup.getTransaction().begin();

        Utente utente = new Utente();
        utente.setKeycloakId("test-keycloak-id");
        utente.setNome("Mario");
        utente.setCognome("Rossi");
        utente.setEmail("mario.rossi@test.it");

        setup.persist(utente);

        Prenotazione prenotazione = Prenotazione.builder()
                .viaggiatore(utente)
                .numeroPartecipanti(1)
                .prezzoTotale(new BigDecimal("100.00"))
                .stato(StatoPrenotazione.IN_ATTESA)
                .dataPrenotazione(LocalDateTime.now())
                .build();

        setup.persist(prenotazione);

        Pagamento pagamento = Pagamento.builder()
                .prenotazione(prenotazione)
                .importo(new BigDecimal("100.00"))
                .stato(StatoPagamento.IN_ATTESA)
                .build();

        setup.persist(pagamento);

        setup.getTransaction().commit();

        Long pagamentoId = pagamento.getId();

        setup.close();


        // 2. Simulo due richieste diverse
        EntityManager em1 = entityManagerFactory.createEntityManager();
        EntityManager em2 = entityManagerFactory.createEntityManager();

        try {

            em1.getTransaction().begin();
            em2.getTransaction().begin();

            Pagamento pagamento1 =
                    em1.find(Pagamento.class, pagamentoId);

            Pagamento pagamento2 =
                    em2.find(Pagamento.class, pagamentoId);


            // Entrambe leggono lo stesso stato iniziale
            assertEquals(StatoPagamento.IN_ATTESA, pagamento1.getStato());
            assertEquals(StatoPagamento.IN_ATTESA, pagamento2.getStato());


            // Entrambe provano a completare il pagamento
            pagamento1.setStato(StatoPagamento.COMPLETATO);
            pagamento2.setStato(StatoPagamento.COMPLETATO);


            // 3. La prima transazione salva
            em1.getTransaction().commit();


            // 4. La seconda lavora ancora sulla vecchia version
            assertThrows(
                    OptimisticLockException.class,
                    em2::flush
            );

        } finally {

            if (em2.getTransaction().isActive()) {
                em2.getTransaction().rollback();
            }

            if (em1.isOpen()) {
                em1.close();
            }

            if (em2.isOpen()) {
                em2.close();
            }
        }


        // 5. Controllo finale
        EntityManager verifica = entityManagerFactory.createEntityManager();

        Pagamento risultato =
                verifica.find(Pagamento.class, pagamentoId);

        assertEquals(StatoPagamento.COMPLETATO, risultato.getStato());
        assertEquals(1L, risultato.getVersion());

        verifica.close();
    }
}