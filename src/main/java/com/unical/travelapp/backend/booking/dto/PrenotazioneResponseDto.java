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

    // Itinerario prenotato: assente per le prenotazioni di una singola attivita'.
    private Long itinerarioId;

    // Date del viaggio (non della prenotazione): stanno sulla partenza scelta o sulla
    // sessione, e il client ne ha bisogno per distinguere i viaggi conclusi da quelli
    // ancora da fare senza rifare i conti su altre chiamate.
    private LocalDateTime dataInizioViaggio;
    private LocalDateTime dataFineViaggio;

    /** true quando la data di fine e' passata e la prenotazione non e' stata cancellata. */
    private boolean conclusa;

    /**
     * true quando l'utente puo' lasciare una recensione adesso: viaggio concluso, collegato
     * a un itinerario e non ancora recensito. Il client non deve ricostruire questa regola:
     * e' la stessa che applica il server quando riceve la POST.
     */
    private boolean recensibile;

    /** Recensione gia' scritta su questa prenotazione, se c'e': serve ad aprirla in modifica. */
    private Long recensioneId;
}
