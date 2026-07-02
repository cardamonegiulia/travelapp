package com.unical.travelapp.backend.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pagamenti")
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(nullable = false)
    private Prenotazione prenotazione;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importo;

    @Column(nullable = false)
    private LocalDateTime dataPagamento;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatoPagamento stato;
}