package com.unical.travelapp.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tappe")
@Data
public class Tappa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tappa")
    private Long id;

    @Column(name = "ordine", nullable = false)
    private Integer ordine;

    @Column(name = "descrizione", length = 1000)
    private String descrizione;

    @Column(name = "localita")
    private String localita;

    @ManyToOne
    @JoinColumn(name = "id_itinerario")
    private Itinerario itinerario;
}