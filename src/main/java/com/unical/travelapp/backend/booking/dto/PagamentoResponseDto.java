package com.unical.travelapp.backend.booking.dto;

import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
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
public class PagamentoResponseDto {

    private Long idPagamento;

    private Long prenotazioneId;

    private BigDecimal importo;

    private StatoPagamento statoPagamento;

    private StatoPrenotazione statoPrenotazione;

    private LocalDateTime dataPagamento;
}