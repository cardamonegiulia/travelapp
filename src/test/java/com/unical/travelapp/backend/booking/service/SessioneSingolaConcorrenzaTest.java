package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class SessioneSingolaConcorrenzaTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // Usa il database isolato previsto per i test
    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.applica(registry);
    }

    @Test
    void deveBloccareDueModificheConcorrentiSullaStessaSessione() {


        EntityManager setup = entityManagerFactory.createEntityManager();

        setup.getTransaction().begin();

        SessioneSingolaAttivita sessione = new SessioneSingolaAttivita();
        sessione.setPostiDisponibili(1);
        sessione.setStato("DISPONIBILE");

        setup.persist(sessione);

        setup.getTransaction().commit();

        Long sessioneId = sessione.getId();

        setup.close();

        EntityManager em1 = entityManagerFactory.createEntityManager();
        EntityManager em2 = entityManagerFactory.createEntityManager();

        try {
            em1.getTransaction().begin();
            em2.getTransaction().begin();
            SessioneSingolaAttivita sessione1 =
                    em1.find(SessioneSingolaAttivita.class, sessioneId);

            SessioneSingolaAttivita sessione2 =
                    em2.find(SessioneSingolaAttivita.class, sessioneId);


            // Entrambe leggono 1 posto disponibile
            assertEquals(1, sessione1.getPostiDisponibili());
            assertEquals(1, sessione2.getPostiDisponibili());

            // Entrambe provano a prendere l'ultimo posto
            sessione1.setPostiDisponibili(
                    sessione1.getPostiDisponibili() - 1
            );

            sessione2.setPostiDisponibili(
                    sessione2.getPostiDisponibili() - 1
            );

            em1.getTransaction().commit();

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

        EntityManager verifica = entityManagerFactory.createEntityManager();

        SessioneSingolaAttivita risultato =
                verifica.find(SessioneSingolaAttivita.class, sessioneId);

        assertEquals(0, risultato.getPostiDisponibili());
        assertEquals(1L, risultato.getVersion());

        verifica.close();
    }
}