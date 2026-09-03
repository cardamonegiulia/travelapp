package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
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
class DisponibilitaConcorrenzaTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.applica(registry);
    }

    @Test
    void deveBloccareDueModificheConcorrentiSullaStessaDisponibilita() {
        EntityManager setup = entityManagerFactory.createEntityManager();

        setup.getTransaction().begin();

        DisponibilitaItinerario disponibilita = new DisponibilitaItinerario();
        disponibilita.setPostiDisponibili(1);

        setup.persist(disponibilita);

        setup.getTransaction().commit();

        Long disponibilitaId = disponibilita.getId();

        setup.close();

        EntityManager em1 = entityManagerFactory.createEntityManager();
        EntityManager em2 = entityManagerFactory.createEntityManager();

        try {

            em1.getTransaction().begin();
            em2.getTransaction().begin();

            // Entrambi leggono la stessa riga
            DisponibilitaItinerario disp1 =
                    em1.find(DisponibilitaItinerario.class, disponibilitaId);

            DisponibilitaItinerario disp2 =
                    em2.find(DisponibilitaItinerario.class, disponibilitaId);


            // Entrambi vedono 1 posto disponibile
            assertEquals(1, disp1.getPostiDisponibili());
            assertEquals(1, disp2.getPostiDisponibili());


            // Entrambi provano a prenotare l'ultimo posto
            disp1.setPostiDisponibili(
                    disp1.getPostiDisponibili() - 1
            );

            disp2.setPostiDisponibili(
                    disp2.getPostiDisponibili() - 1
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

        DisponibilitaItinerario risultato =
                verifica.find(DisponibilitaItinerario.class, disponibilitaId);

        // È stata accettata una sola prenotazione
        assertEquals(0, risultato.getPostiDisponibili());

        // La prima modifica deve aver incrementato @Version
        assertEquals(1L, risultato.getVersion());
        verifica.close();
    }
}