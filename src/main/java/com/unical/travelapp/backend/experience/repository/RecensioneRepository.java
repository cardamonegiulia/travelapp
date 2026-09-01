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

    // tutte le recensioni di un itinerario (paginato). L'autore viene caricato insieme alla
    // riga: la risposta ne espone nome e cognome, e senza entity graph sarebbe una query in
    // piu' per ogni recensione dell'elenco.
    @EntityGraph(attributePaths = "utente")
    Page<Recensione> findByItinerario_Id(Long itinerarioId, Pageable pageable);

    // usato per il calcolo della media voti: qui serve l'intera lista, non paginata
    List<Recensione> findByItinerario_Id(Long itinerarioId);

    /**
     * Le recensioni scritte da un utente (paginato), per la sezione "Le mie recensioni".
     *
     * <p>L'itinerario viene caricato insieme alla riga: l'elenco mostra il titolo del
     * viaggio recensito, e senza entity graph sarebbe una query in piu' per ogni recensione.
     */
    @EntityGraph(attributePaths = {"utente", "itinerario"})
    Page<Recensione> findByUtente_Id(Long utenteId, Pageable pageable);

    // un utente non deve poter recensire la stessa prenotazione due volte
    boolean existsByPrenotazione(Prenotazione prenotazione);

    /** La recensione (al massimo una) lasciata su una prenotazione. */
    Optional<Recensione> findByPrenotazione_Id(Long prenotazioneId);

    /**
     * Le recensioni gia' scritte su un gruppo di prenotazioni: serve a marcare in un colpo
     * solo quali viaggi conclusi sono ancora da recensire, senza una query per riga.
     */
    List<Recensione> findByPrenotazione_IdIn(Collection<Long> prenotazioneIds);

    /**
     * Media e numero di recensioni per un gruppo di itinerari.
     *
     * <p>Gli itinerari senza recensioni non compaiono nel risultato: chi legge distingue
     * cosi' "nessuna recensione" da "media pari a zero", che non e' un voto possibile.
     */
    @Query("""
            select r.itinerario.id as itinerarioId,
                   avg(r.voto) as media,
                   count(r) as numero
            from Recensione r
            where r.itinerario.id in :itinerarioIds
            group by r.itinerario.id
            """)
    List<StatisticheRecensioni> statistichePerItinerari(@Param("itinerarioIds") Collection<Long> itinerarioIds);

    /** Proiezione read-only con le due sole colonne aggregate che servono alle anteprime. */
    interface StatisticheRecensioni {
        Long getItinerarioId();

        Double getMedia();

        Long getNumero();
    }
}
