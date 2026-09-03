package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.experience.models.Notifica;
import com.unical.travelapp.backend.experience.models.TipoNotifica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificaRepository extends JpaRepository<Notifica, Long> {

    Page<Notifica> findByDestinatario_IdOrderByIdDesc(Long destinatarioId, Pageable pageable);

    Optional<Notifica> findByIdAndDestinatario_Id(Long id, Long destinatarioId);

    long countByDestinatario_IdAndLettaFalse(Long destinatarioId);

    boolean existsByPrenotazione_IdAndTipo(Long prenotazioneId, TipoNotifica tipo);


    @org.springframework.data.jpa.repository.Query("""
            select n.prenotazione.id from Notifica n
            where n.tipo = :tipo and n.prenotazione.id in :prenotazioneIds
            """)
    List<Long> findPrenotazioneIdsConNotifica(
            @org.springframework.data.repository.query.Param("tipo") TipoNotifica tipo,
            @org.springframework.data.repository.query.Param("prenotazioneIds") Collection<Long> prenotazioneIds);


    List<Notifica> findByPrenotazione_IdAndTipo(Long prenotazioneId, TipoNotifica tipo);
}
