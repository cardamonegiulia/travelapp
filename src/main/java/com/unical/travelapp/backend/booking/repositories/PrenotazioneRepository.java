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

    // --- Viaggi conclusi / in corso ------------------------------------------------------
    //
    // La data di fine non sta sulla prenotazione: sta sulla disponibilita' dell'itinerario
    // oppure sulla sessione della singola attivita', e ogni prenotazione ne ha valorizzata
    // esattamente una. Le join sono LEFT di proposito: con la navigazione implicita
    // (p.disponibilitaItinerario.dataFine) Hibernate genera INNER JOIN e le prenotazioni di
    // attivita' singole sparirebbero dal risultato.
    //
    // Le prenotazioni CANCELLATE non compaiono fra i viaggi conclusi: il viaggio non e' mai
    // stato fatto, quindi non c'e' niente da recensire.

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

    /**
     * Prenotazioni di itinerario la cui data di fine cade in [da, a): usata dal job che
     * invita a recensire. Solo itinerari, perche' la recensione e' agganciata a un
     * itinerario e una sessione di attivita' singola non ne ha uno.
     */
    // Le join sono "fetch" perche' il job gira fuori da una richiesta HTTP: viaggiatore e
    // itinerario servono subito dopo, e senza caricarli qui verrebbero risolti a sessione
    // gia' chiusa.
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
}
