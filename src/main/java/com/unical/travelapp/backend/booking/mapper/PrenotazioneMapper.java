package com.unical.travelapp.backend.booking.mapper;

import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class PrenotazioneMapper {
    private final PagamentoRepository pagamentoRepository;

    public PrenotazioneResponseDto toResponseDto(Prenotazione prenotazione) {
        Pagamento pagamento = pagamentoRepository
                .findByPrenotazioneId(prenotazione.getId())
                .orElse(null);
        return PrenotazioneResponseDto.builder()
                .id(prenotazione.getId())
                .viaggiatoreId(prenotazione.getViaggiatore().getId())
                .nomeViaggiatore(prenotazione.getViaggiatore().getNome())
                .cognomeViaggiatore(prenotazione.getViaggiatore().getCognome())
                .disponibilitaItinerarioId(prenotazione.getDisponibilitaItinerario() != null ? prenotazione.getDisponibilitaItinerario().getId() : null)
                .sessioneSingolaAttivitaId(prenotazione.getSessioneSingolaAttivita() != null ? prenotazione.getSessioneSingolaAttivita().getId() : null)
                .destinazione(prenotazione.getDisponibilitaItinerario() != null ? prenotazione.getDisponibilitaItinerario().getItinerario().getDestinazionePrincipale() : null)
                .numeroPartecipanti(prenotazione.getNumeroPartecipanti()).prezzoTotale(prenotazione.getPrezzoTotale()).statoPrenotazione(prenotazione.getStato())
                .statoPagamento(pagamento != null ? pagamento.getStato() : null)
                .dataPrenotazione(prenotazione.getDataPrenotazione())
                .build();
    }
    public List<PrenotazioneResponseDto> toListResponseDto(List<Prenotazione> prenotazioni) {
        //con prenotazioni.stream() scorro la lista, map(...) trasformo ogni Prenotazione in una PrenotazioneResponseDto, e infine con toList ricreo una lista finale di DTO
        return prenotazioni.stream().map(this::toResponseDto).toList();
    }
}