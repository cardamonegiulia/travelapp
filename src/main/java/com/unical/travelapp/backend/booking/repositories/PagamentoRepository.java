package com.unical.travelapp.backend.booking.repositories;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByPrenotazioneId(Long prenotazioneId);

    List<Pagamento> findByPrenotazioneIdIn(List<Long> prenotazioneIds);

    Page<Pagamento> findByPrenotazioneViaggiatoreId(
            Long viaggiatoreId,
            Pageable pageable
    );

    @Query("""
            select coalesce(sum(pg.importo), 0) from Pagamento pg
            join pg.prenotazione p
            where pg.stato = :statoPagamento
              and p.stato <> :statoPrenotazioneEscluso
            """)
    BigDecimal sumIncassatoGlobale(@Param("statoPagamento") StatoPagamento statoPagamento,
                                   @Param("statoPrenotazioneEscluso") StatoPrenotazione statoPrenotazioneEscluso);

    @Query("""
            select coalesce(sum(pg.importo), 0) from Pagamento pg
            join pg.prenotazione p
            left join p.disponibilitaItinerario d
            left join d.itinerario i
            left join i.organizzatore oi
            left join p.sessioneSingolaAttivita s
            left join s.singolaAttivita a
            left join a.organizzatore oa
            where pg.stato = :statoPagamento
              and p.stato <> :statoPrenotazioneEscluso
              and (oi.id = :organizzatoreId or oa.id = :organizzatoreId)
            """)
    BigDecimal sumIncassatoPerOrganizzatore(@Param("organizzatoreId") Long organizzatoreId,
                                            @Param("statoPagamento") StatoPagamento statoPagamento,
                                            @Param("statoPrenotazioneEscluso") StatoPrenotazione statoPrenotazioneEscluso);
}