package com.unical.travelapp.backend.identity.repository;

import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtenteRepository extends JpaRepository<Utente, Long> {

    // Metodo fondamentale per il futuro: trova l'utente tramite l'ID di Keycloak
    Optional<Utente> findByKeycloakId(String keycloakId);

    // Metodo utile per verificare se un'email è già registrata
    Optional<Utente> findByEmail(String email);
}