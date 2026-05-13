package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Utente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "preferiti")
public class preferito {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @OneToMany
    @NotNull
    @JoinColumn(name = "utente_id")
    private Utente utente;

    @ManyToMany
    @JoinColumn(name = "itinerario_id")
    private Itinerario itinerario;
}
