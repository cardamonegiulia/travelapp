package com.unical.travelapp.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "tappe")
@Data
public class Tappa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "itinerario_id")
    private Itinerario itinerario;

    @Column(name = "nome_citta")
    private String nomeCitta;

    private Integer ordine;

    @Column(name = "giorni_permanenza")
    private Integer giorniPermanenza;

    @OneToMany(mappedBy = "tappa", cascade = CascadeType.ALL)
    private List<Attivita> attivita;
}