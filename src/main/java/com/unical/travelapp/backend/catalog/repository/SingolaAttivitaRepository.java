package com.unical.travelapp.backend.catalog.repository;

import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SingolaAttivitaRepository extends JpaRepository<SingolaAttivita, Long> {
}