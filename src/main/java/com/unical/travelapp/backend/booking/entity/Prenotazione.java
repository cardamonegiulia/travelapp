package com.unical.travelapp.backend.booking.entity;

import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;


// uso lombok che ci evita codice noioso come getter, setter, costruttore ecc
//con Data genera automaitcamente getter e setter ecc
// NOArgs costruttore vuoto
//All arg il contrario costurttore con tutti i campi

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prenotazioni")
public class Prenotazione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Utente viaggiatore;

    @ManyToOne
    @JoinColumn(nullable = true)
    private DisponibilitaItinerario disponibilitaItinerario;

    @ManyToOne
    @JoinColumn(nullable = true)
    private SessioneSingolaAttivita sessioneSingolaAttivita;

    @Column(nullable = false)
    private Integer numeroPartecipanti;

    @Column(nullable = false)
    private BigDecimal prezzoTotale;

    @Enumerated(EnumType.STRING)
    private StatoPrenotazione stato;

    @Column(nullable = false)
    private LocalDateTime dataPrenotazione;

}
