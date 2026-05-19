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
public class PrenotazioneResponseDto {
    private Long id;                           // id prenotazione
    private Long viaggiatoreId;                // id utente
    private String nomeViaggiatore;            // nome utente
    private String cognomeViaggiatore;         // cognome utente
    private Long disponibilitaItinerarioId;    // id disponibilità itinerario
    private Long sessioneSingolaAttivitaId;   // id sessione singola se presente
    private String destinazione;               // destinazione dell’itinerario
    private Integer numeroPartecipanti;        // posti prenotati
    private BigDecimal prezzoTotale;           // prezzo totale prenotazione
    private StatoPrenotazione statoPrenotazione; // stato prenotazione
    private StatoPagamento statoPagamento;     // stato pagamento
    private LocalDateTime dataPrenotazione;    // data di creazione prenotazione
}