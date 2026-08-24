package com.unical.travelapp.backend.catalog.repository;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisponibilitaItinerarioRepository extends JpaRepository<DisponibilitaItinerario, Long> {
    List<DisponibilitaItinerario> findByItinerario_Id(Long itinerarioId);
}