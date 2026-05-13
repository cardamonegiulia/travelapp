package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.identity.entity.Utente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "recensioni")
public class recensione {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long Id;

    @ManyToOne
    @JoinColumn(name = "prenotazione_id")
    private Prenotazione prenotazione;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "autore_id")
    private Utente utente;

    private int voto;

    private String commento;
}
