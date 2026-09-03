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

    List<ListaPreferiti> findByUtenteOrderByIdDesc(Utente utente);

    Optional<ListaPreferiti> findFirstByUtenteAndNome(Utente utente, String nome);

    boolean existsByUtenteAndNomeIgnoreCase(Utente utente, String nome);


    @Query("""
            select l from ListaPreferiti l
            join l.destinatari d
            where d = :destinatario
              and l.visibilita = com.unical.travelapp.backend.experience.models.VisibilitaListaPreferiti.CONDIVISA
            order by l.id desc
            """)
    List<ListaPreferiti> findCondiviseCon(@Param("destinatario") Utente destinatario);
}
