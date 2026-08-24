package com.unical.travelapp.backend.booking.repositories;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {
    Page<Prenotazione> findByViaggiatoreId(Long viaggiatoreId, Pageable pageable);

    Optional<Prenotazione> findByIdAndViaggiatoreId(Long id, Long viaggiatoreId);

    @Query("SELECT COALESCE(SUM(p.prezzoTotale), 0) FROM Prenotazione p WHERE p.stato <> :statoEscluso")
    BigDecimal sumTotaleGlobale(@Param("statoEscluso") StatoPrenotazione statoEscluso);

    @Query("SELECT COALESCE(SUM(p.prezzoTotale), 0) FROM Prenotazione p " +
            "WHERE p.stato <> :statoEscluso AND (" +
            "p.disponibilitaItinerario.itinerario.organizzatore.id = :organizzatoreId OR " +
            "p.sessioneSingolaAttivita.singolaAttivita.organizzatore.id = :organizzatoreId)")
    BigDecimal sumTotalePerOrganizzatore(@Param("organizzatoreId") Long organizzatoreId,
                                         @Param("statoEscluso") StatoPrenotazione statoEscluso);
}