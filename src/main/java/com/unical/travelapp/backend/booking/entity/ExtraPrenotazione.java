package com.unical.travelapp.backend.booking.entity;


import com.unical.travelapp.backend.catalog.entity.Attivita;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;


@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "extra_prenotazioni", uniqueConstraints = @UniqueConstraint(columnNames = {"prenotazione_id", "attivita_id"}))
public class ExtraPrenotazione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Prenotazione prenotazione;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Attivita attivita;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoExtra;

}
