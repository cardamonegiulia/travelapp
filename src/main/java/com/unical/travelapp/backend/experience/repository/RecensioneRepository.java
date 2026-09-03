package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.experience.models.Recensione;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecensioneRepository extends JpaRepository<Recensione, Long> {

    @EntityGraph(attributePaths = "utente")
    Page<Recensione> findByItinerario_Id(Long itinerarioId, Pageable pageable);

    List<Recensione> findByItinerario_Id(Long itinerarioId);

    @EntityGraph(attributePaths = {"utente", "itinerario"})
    Page<Recensione> findByUtente_Id(Long utenteId, Pageable pageable);

    boolean existsByPrenotazione(Prenotazione prenotazione);

    Optional<Recensione> findByPrenotazione_Id(Long prenotazioneId);

    List<Recensione> findByPrenotazione_IdIn(Collection<Long> prenotazioneIds);

    @Query("""
            select r.itinerario.id as itinerarioId,
                   avg(r.voto) as media,
                   count(r) as numero
            from Recensione r
            where r.itinerario.id in :itinerarioIds
            group by r.itinerario.id
            """)
    List<StatisticheRecensioni> statistichePerItinerari(@Param("itinerarioIds") Collection<Long> itinerarioIds);

    interface StatisticheRecensioni {
        Long getItinerarioId();

        Double getMedia();

        Long getNumero();
    }
}
