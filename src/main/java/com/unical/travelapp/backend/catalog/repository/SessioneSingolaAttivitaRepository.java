package com.unical.travelapp.backend.catalog.repository;

import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessioneSingolaAttivitaRepository extends JpaRepository<SessioneSingolaAttivita, Long> {
}