package com.unical.travelapp.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "itinerari")
@Data
public class Itinerario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_itinerario")
    private Long id;

    @Column(name = "titolo", nullable = false)
    private String titolo;

    @Column(name = "descrizione", length = 1000)
    private String descrizione;

    @Column(name = "prezzo_base")
    private Double prezzoBase;

}