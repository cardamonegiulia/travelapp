package com.unical.travelapp.backend.booking.mapper;

import com.unical.travelapp.backend.booking.dto.PrenotazioneResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.service.PagamentoService;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PrenotazioneAssembler {
    private final PrenotazioneMapper mapper;
    private final PagamentoService pagamentoService;
    private final RecensioneService recensioneService;
    public PrenotazioneAssembler(PrenotazioneMapper mapper,
                                 PagamentoService pagamentoService,
                                 RecensioneService recensioneService) {
        this.mapper = mapper;
        this.pagamentoService = pagamentoService;
        this.recensioneService = recensioneService;
    }
    public Page<PrenotazioneResponseDto> assembla(Page<Prenotazione> prenotazioni) {
        List<Long> ids = prenotazioni.getContent().stream().map(Prenotazione::getId).toList();
        Map<Long, Pagamento> pagamenti = pagamentoService.getPagamentiPerPrenotazioni(ids);
        Map<Long, Long> recensioni = recensioneService.getRecensioniPerPrenotazioni(ids);
        return mapper.toResponseDtoPage(prenotazioni, pagamenti, recensioni);
    }
    public PrenotazioneResponseDto assembla(Prenotazione prenotazione) {
        Pagamento pagamento = pagamentoService.getPagamentoPrenotazione(prenotazione.getId());
        Long recensioneId = recensioneService
                .getRecensioniPerPrenotazioni(List.of(prenotazione.getId()))
                .get(prenotazione.getId());
        return mapper.toResponseDto(prenotazione, pagamento, recensioneId);
    }
}
