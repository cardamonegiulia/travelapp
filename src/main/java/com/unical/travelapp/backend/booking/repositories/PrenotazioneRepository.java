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
import java.time.LocalDateTime;
import java.util.List;
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

    @Query("""
            select p from Prenotazione p
            left join p.disponibilitaItinerario d
            left join p.sessioneSingolaAttivita s
            where p.viaggiatore.id = :viaggiatoreId
              and p.stato <> :statoEscluso
              and coalesce(d.dataFine, s.dataFine) < :istante
            order by coalesce(d.dataFine, s.dataFine) desc
            """)
    Page<Prenotazione> findConcluseByViaggiatore(@Param("viaggiatoreId") Long viaggiatoreId,
                                                 @Param("istante") LocalDateTime istante,
                                                 @Param("statoEscluso") StatoPrenotazione statoEscluso,
                                                 Pageable pageable);

    // Tutto cio' che non e' concluso: viaggi in corso, futuri e anche le prenotazioni
    // cancellate, che l'utente deve continuare a vedere nella lista principale.
    @Query("""
            select p from Prenotazione p
            left join p.disponibilitaItinerario d
            left join p.sessioneSingolaAttivita s
            where p.viaggiatore.id = :viaggiatoreId
              and (p.stato = :statoEscluso
                   or coalesce(d.dataFine, s.dataFine) is null
                   or coalesce(d.dataFine, s.dataFine) >= :istante)
            """)
    Page<Prenotazione> findAttualiByViaggiatore(@Param("viaggiatoreId") Long viaggiatoreId,
                                                @Param("istante") LocalDateTime istante,
                                                @Param("statoEscluso") StatoPrenotazione statoEscluso,
                                                Pageable pageable);

    @Query(value = """
            select p from Prenotazione p
            join fetch p.viaggiatore
            where p.disponibilitaItinerario.id = :disponibilitaId
              and p.stato <> :statoEscluso
            """,
            countQuery = """
            select count(p) from Prenotazione p
            where p.disponibilitaItinerario.id = :disponibilitaId
              and p.stato <> :statoEscluso
            """)
    Page<Prenotazione> findByDisponibilitaItinerario(@Param("disponibilitaId") Long disponibilitaId,
                                                    @Param("statoEscluso") StatoPrenotazione statoEscluso,
                                                    Pageable pageable);

    boolean existsByDisponibilitaItinerario_Id(Long disponibilitaId);

    @Query("""
            select p.disponibilitaItinerario.id, count(p), coalesce(sum(p.numeroPartecipanti), 0)
            from Prenotazione p
            where p.disponibilitaItinerario.id in :disponibilitaIds
              and p.stato <> :statoEscluso
            group by p.disponibilitaItinerario.id
            """)
    List<Object[]> contaPerDisponibilita(@Param("disponibilitaIds") List<Long> disponibilitaIds,
                                         @Param("statoEscluso") StatoPrenotazione statoEscluso);

    @Query("""
            select p from Prenotazione p
            join fetch p.disponibilitaItinerario d
            join fetch d.itinerario
            join fetch p.viaggiatore
            where p.stato <> :statoEscluso
              and d.dataFine >= :da
              and d.dataFine < :a
            """)
    List<Prenotazione> findItinerariConclusiTra(@Param("da") LocalDateTime da,
                                                @Param("a") LocalDateTime a,
                                                @Param("statoEscluso") StatoPrenotazione statoEscluso);

    @Query("""
        select p from Prenotazione p
        join fetch p.disponibilitaItinerario d
        join fetch d.itinerario
        join fetch p.viaggiatore
        where p.stato <> :statoEscluso
          and d.dataFine < :primaDi
        order by d.dataFine asc
        """)
    List<Prenotazione> findItinerariConclusiPrimaDi(
            @Param("primaDi") LocalDateTime primaDi,
            @Param("statoEscluso") StatoPrenotazione statoEscluso
    );
}

