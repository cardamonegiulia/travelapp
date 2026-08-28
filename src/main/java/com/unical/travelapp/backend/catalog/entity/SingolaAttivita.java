package com.unical.travelapp.backend.catalog.entity;

import com.unical.travelapp.backend.common.audit.Auditable;
import com.unical.travelapp.backend.experience.models.Immagine;
import com.unical.travelapp.backend.identity.entity.Utente;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "singole_attivita")
@Data
@EqualsAndHashCode(callSuper = false)
public class SingolaAttivita extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organizzatore_id")
    private Utente organizzatore;

    private String titolo;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    private String luogo;

    private BigDecimal prezzo;

    @Column(name = "durata_minuti")
    private Integer durataMinuti;

    @Column(name = "max_partecipanti")
    private Integer maxPartecipanti;

    @OneToMany(mappedBy = "singolaAttivita", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessioneSingolaAttivita> sessioni;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "singola_attivita_id")
    private List<Immagine> immagini = new ArrayList<>();
}