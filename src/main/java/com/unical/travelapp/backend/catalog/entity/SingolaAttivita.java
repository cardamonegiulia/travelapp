package com.unical.travelapp.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
//import com.unical.travelapp.backend.identity.entity.Utente;

@Entity
@Table(name = "singole_attivita")
@Data
public class SingolaAttivita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @ManyToOne
//    @JoinColumn(name = "organizzatore_id")
//    private Utente organizzatore;

    private String titolo;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    private String luogo;

    private BigDecimal prezzo;

    @Column(name = "durata_minuti")
    private Integer durataMinuti;

    @Column(name = "max_partecipanti")
    private Integer maxPartecipanti;
}