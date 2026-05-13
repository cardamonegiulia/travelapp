package com.unical.travelapp.backend.booking.repositories;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {
}
