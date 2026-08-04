package com.unical.travelapp.backend.booking.repositories;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {
    Page<Prenotazione> findByViaggiatoreId(Long viaggiatoreId, Pageable pageable);

    Optional<Prenotazione> findByIdAndViaggiatoreId(Long id, Long viaggiatoreId);
}
