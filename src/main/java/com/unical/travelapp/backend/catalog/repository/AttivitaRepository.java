package com.unical.travelapp.backend.catalog.repository;

import com.unical.travelapp.backend.catalog.entity.Attivita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttivitaRepository extends JpaRepository<Attivita, Long> {
}