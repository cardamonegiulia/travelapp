package com.unical.travelapp.backend.catalog.repository;
import com.unical.travelapp.backend.catalog.entity.Attivita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface AttivitaRepository extends JpaRepository<Attivita, Long> {
    @Query("""
        SELECT a
        FROM Attivita a
        JOIN a.tappa t
        WHERE t.itinerario.id = :itinerarioId
          AND a.obbligatoria = false
          AND a.prezzoExtra IS NOT NULL
        ORDER BY t.ordine ASC, a.orarioSvolgimento ASC
    """)
    List<Attivita> findExtraByItinerarioId(
            @Param("itinerarioId") Long itinerarioId
    );
}
