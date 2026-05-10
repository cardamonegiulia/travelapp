package com.unical.travelapp.backend.identity.models;

import jakarta.persistence.*;

@Entity
@Table(name = "recensioni")
public class recensione {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long Id;

    private long prenotazione_id;

    private long autore_id;

    private int voto;

    private String commento;
}
