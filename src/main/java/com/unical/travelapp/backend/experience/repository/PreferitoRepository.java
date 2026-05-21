package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.experience.models.Preferito;
import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreferitoRepository extends JpaRepository<Preferito, Long> {

    Preferito findByUtente (Utente utente);
}
