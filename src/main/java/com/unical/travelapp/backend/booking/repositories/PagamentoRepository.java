package com.unical.travelapp.backend.booking.repositories;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByPrenotazioneId(Long prenotazioneId);

    List<Pagamento> findByPrenotazioneIdIn(List<Long> prenotazioneIds);

    // Recupera lo storico dei pagamenti associati alle prenotazioni di un utente.
    Page<Pagamento> findByPrenotazioneViaggiatoreId(
            Long viaggiatoreId,
            Pageable pageable
    );
}