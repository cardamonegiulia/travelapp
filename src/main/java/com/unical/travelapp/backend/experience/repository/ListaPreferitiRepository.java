package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.experience.models.ListaPreferiti;
import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListaPreferitiRepository extends JpaRepository<ListaPreferiti, Long> {

    /** Tutte le liste di cui l'utente e' proprietario, dalla piu' recente. */
    List<ListaPreferiti> findByUtenteOrderByIdDesc(Utente utente);

    /** La lista "I miei preferiti" creata implicitamente, se esiste gia'. */
    Optional<ListaPreferiti> findFirstByUtenteAndNome(Utente utente, String nome);

    boolean existsByUtenteAndNomeIgnoreCase(Utente utente, String nome);

    /**
     * Le liste che ALTRI utenti hanno condiviso con {@code destinatario}.
     *
     * <p>Il filtro sulla visibilita' e' ridondante rispetto all'invariante mantenuta dal
     * service (una lista privata non ha destinatari), ma e' la garanzia che una riga
     * rimasta indietro non renda comunque leggibile una lista tornata privata.
     */
    @Query("""
            select l from ListaPreferiti l
            join l.destinatari d
            where d = :destinatario
              and l.visibilita = com.unical.travelapp.backend.experience.models.VisibilitaListaPreferiti.CONDIVISA
            order by l.id desc
            """)
    List<ListaPreferiti> findCondiviseCon(@Param("destinatario") Utente destinatario);
}
