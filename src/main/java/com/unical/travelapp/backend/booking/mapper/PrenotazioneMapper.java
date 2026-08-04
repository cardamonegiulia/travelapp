package com.unical.travelapp.backend.booking.mapper;

import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PrenotazioneMapper {
    private final PagamentoRepository pagamentoRepository;

    public PrenotazioneResponseDto toResponseDto(Prenotazione prenotazione) {
        Pagamento pagamento = pagamentoRepository
                .findByPrenotazioneId(prenotazione.getId())
                .orElse(null);

        String tipoPrenotazione;
        String titolo;
        String luogo;

        if (prenotazione.getDisponibilitaItinerario() != null) {
            tipoPrenotazione = "ITINERARIO";
            titolo = prenotazione.getDisponibilitaItinerario().getItinerario().getTitolo();
            luogo = prenotazione.getDisponibilitaItinerario().getItinerario().getDestinazionePrincipale();
        } else {
            tipoPrenotazione = "SESSIONE_SINGOLA";
            titolo = prenotazione.getSessioneSingolaAttivita().getSingolaAttivita().getTitolo();
            luogo = prenotazione.getSessioneSingolaAttivita().getSingolaAttivita().getLuogo();
        }

        return PrenotazioneResponseDto.builder()
                .id(prenotazione.getId())
                .viaggiatoreId(prenotazione.getViaggiatore().getId())
                .nomeViaggiatore(prenotazione.getViaggiatore().getNome())
                .cognomeViaggiatore(prenotazione.getViaggiatore().getCognome())
                .disponibilitaItinerarioId(prenotazione.getDisponibilitaItinerario() != null ? prenotazione.getDisponibilitaItinerario().getId() : null)
                .sessioneSingolaAttivitaId(prenotazione.getSessioneSingolaAttivita() != null ? prenotazione.getSessioneSingolaAttivita().getId() : null)
                .numeroPartecipanti(prenotazione.getNumeroPartecipanti()).prezzoTotale(prenotazione.getPrezzoTotale()).statoPrenotazione(prenotazione.getStato())
                .statoPagamento(pagamento != null ? pagamento.getStato() : null)
                .dataPrenotazione(prenotazione.getDataPrenotazione())
                .tipoPrenotazione(tipoPrenotazione)
                .titolo(titolo)
                .luogo(luogo)
                .destinazione(luogo)
                .build();
    }
}