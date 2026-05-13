package com.unical.travelapp.backend.booking.repositories;

import com.unical.travelapp.backend.booking.entity.ExtraPrenotazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtraPrenotazioneRepository extends JpaRepository<ExtraPrenotazione, Long> {
}
