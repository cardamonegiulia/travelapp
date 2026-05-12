package com.unical.travelapp.backend.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pagamenti")
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(nullable = false)
    private Prenotazione prenotazione;

    @Column(nullable = false)
    private BigDecimal importo;

    @Column(nullable = false)
    private Timestamp dataPagamento;

    @Enumerated(EnumType.STRING)
    private StatoPagamento stato;
}
