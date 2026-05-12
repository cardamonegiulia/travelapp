package com.unical.travelapp.backend.booking.entity;

import com.unical.travelapp.backend.identity.entity.Utente;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prenotazioni")
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="viaggiatore_id", nullable=false)
    private Utente viaggiatore;

    @Column(nullable = false)
    private Integer numeroPartecipanti;

    @Column(nullable = false)
    private BigDecimal prezzoTotale;

    @Enumerated(EnumType.STRING)
    private StatoPrenotazione stato;

    @Column(nullable = false)
    private LocalDateTime dataPrenotazione;

}
