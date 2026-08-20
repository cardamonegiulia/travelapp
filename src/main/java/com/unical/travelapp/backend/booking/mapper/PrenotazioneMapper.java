package com.unical.travelapp.backend.booking.mapper;

import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.TipoPrenotazione;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PrenotazioneMapper {
    public PrenotazioneResponseDto toResponseDto(Prenotazione prenotazione, Pagamento pagamento) {
        TipoPrenotazione tipoPrenotazione;
        String titolo;
        String luogo;

        if (prenotazione.getDisponibilitaItinerario() != null) {
            tipoPrenotazione = TipoPrenotazione.ITINERARIO;
            titolo = prenotazione.getDisponibilitaItinerario()
                    .getItinerario()
                    .getTitolo();
            luogo = prenotazione.getDisponibilitaItinerario()
                    .getItinerario()
                    .getDestinazionePrincipale();

        } else if (prenotazione.getSessioneSingolaAttivita() != null) {
            tipoPrenotazione = TipoPrenotazione.SESSIONE_SINGOLA;
            titolo = prenotazione.getSessioneSingolaAttivita()
                    .getSingolaAttivita()
                    .getTitolo();
            luogo = prenotazione.getSessioneSingolaAttivita()
                    .getSingolaAttivita()
                    .getLuogo();

        } else {
            throw new IllegalStateException(
                    "Prenotazione senza itinerario o sessione singola: "
                            + prenotazione.getId()
            );
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

    public Page<PrenotazioneResponseDto> toResponseDtoPage(Page<Prenotazione> prenotazioni, Map<Long, Pagamento> pagamenti) {
        return prenotazioni.map(prenotazione ->
                toResponseDto(
                        prenotazione,
                        pagamenti.get(prenotazione.getId())
                )
        );
    }
}