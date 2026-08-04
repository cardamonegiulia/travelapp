package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.experience.models.Recensione;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecensioneRepository extends JpaRepository<Recensione, Long> {

    // tutte le recensioni di un itinerario (paginato)
    Page<Recensione> findByItinerario_Id(Long itinerarioId, Pageable pageable);

    // usato per il calcolo della media voti: qui serve l'intera lista, non paginata
    List<Recensione> findByItinerario_Id(Long itinerarioId);

    // un utente non deve poter recensire la stessa prenotazione due volte
    boolean existsByPrenotazione(Prenotazione prenotazione);
}
