package com.unical.travelapp.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import com.unical.travelapp.backend.identity.entity.Utente;

@Entity
@Table(name = "itinerari")
@Data
public class Itinerario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organizzatore_id")
    private Utente organizzatore;

    private String titolo;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    @Column(name = "destinazione_principale")
    private String destinazionePrincipale;

    @Column(name = "prezzo_base")
    private BigDecimal prezzoBase;

    @Column(name = "durata_giorni")
    private Integer durataGiorni;

    @Column(name = "max_partecipanti")
    private Integer maxPartecipanti;

    private String stato;

    @OneToMany(mappedBy = "itinerario", cascade = CascadeType.ALL)
    private List<Tappa> tappe;

    @OneToMany(mappedBy = "itinerario", cascade = CascadeType.ALL)
    private List<DisponibilitaItinerario> disponibilita;
}