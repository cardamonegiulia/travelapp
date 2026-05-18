package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.identity.entity.Utente;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@Table(name = "recensioni")
public class Recensione {

    @Id
    @Column(name = "recensione_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long Id;

    @ManyToOne
    @JoinColumn(name = "prenotazione_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Prenotazione prenotazione;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "autore_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Utente utente;

    private int voto;

    private String commento;
}
