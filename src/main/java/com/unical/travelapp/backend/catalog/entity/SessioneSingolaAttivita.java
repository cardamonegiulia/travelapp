package com.unical.travelapp.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessioni_singole_attivita")
@Data
public class SessioneSingolaAttivita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "singola_attivita_id")
    private SingolaAttivita singolaAttivita;

    @Column(name = "data_inizio")
    private LocalDateTime dataInizio;

    @Column(name = "data_fine")
    private LocalDateTime dataFine;

    @Column(name = "posti_disponibili")
    private Integer postiDisponibili;

    private String stato;
}