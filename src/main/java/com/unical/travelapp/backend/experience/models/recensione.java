package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.identity.entity.Utente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "recensioni")
public class recensione {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long Id;

    @OneToMany
    @JoinColumn(name = "prenotazione_id")
    private Prenotazione prenotazione;

    @OneToMany
    @NotNull
    @JoinColumn(name = "autore_id")
    private Utente utente;

    private int voto;

    private String commento;
}
