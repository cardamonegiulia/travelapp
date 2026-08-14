package com.unical.travelapp.backend.booking.entity;

import com.unical.travelapp.backend.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pagamenti")
public class Pagamento extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne
    @JoinColumn(nullable = false, unique = true)
    private Prenotazione prenotazione;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importo;

    @Column
    private LocalDateTime dataPagamento;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatoPagamento stato;
}