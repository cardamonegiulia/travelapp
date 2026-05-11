package com.unical.travelapp.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "attivita")
@Data
public class Attivita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tappa_id")
    private Tappa tappa;

    private String titolo;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    private boolean obbligatoria;

    @Column(name = "prezzo_extra")
    private BigDecimal prezzoExtra;

    @Column(name = "orario_svolgimento")
    private LocalTime orarioSvolgimento;
}