package com.unical.travelapp.backend.booking.dto;

import com.unical.travelapp.backend.booking.entity.StatoPagamento;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.entity.TipoPrenotazione;
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
public class PrenotazioneResponseDto {
    private Long id;
    private Long viaggiatoreId;
    private String nomeViaggiatore;
    private String cognomeViaggiatore;
    private Long disponibilitaItinerarioId;
    private Long sessioneSingolaAttivitaId;
    private String destinazione;
    private Integer numeroPartecipanti;
    private BigDecimal prezzoTotale;
    private StatoPrenotazione statoPrenotazione;
    private StatoPagamento statoPagamento;
    private LocalDateTime dataPrenotazione;
    private TipoPrenotazione tipoPrenotazione;
    private String titolo;
    private String luogo;
}