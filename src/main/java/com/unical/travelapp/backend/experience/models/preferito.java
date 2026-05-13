package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Utente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Entity
@Table(name = "preferiti")
public class preferito {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "utente_id")
    private Utente utente;

    @ManyToMany
    @JoinTable(
            name = "itinerario_preferito",
            joinColumns = @JoinColumn(name = "preferito_id"),
            inverseJoinColumns = @JoinColumn(name = "itinerario_id")
    )
    private List<Itinerario> itinerario;
}
