package com.unical.travelapp.backend.catalog.repository;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisponibilitaItinerarioRepository extends JpaRepository<DisponibilitaItinerario, Long> {
}