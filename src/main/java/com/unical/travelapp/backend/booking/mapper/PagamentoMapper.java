package com.unical.travelapp.backend.booking.mapper;

import com.unical.travelapp.backend.booking.dto.PagamentoResponseDto;
import com.unical.travelapp.backend.booking.entity.Pagamento;
import org.springframework.stereotype.Component;

@Component
public class PagamentoMapper {

    public PagamentoResponseDto toResponseDto(Pagamento pagamento) {

        return PagamentoResponseDto.builder()
                .idPagamento(pagamento.getId())
                .prenotazioneId(pagamento.getPrenotazione().getId())
                .importo(pagamento.getImporto())
                .statoPagamento(pagamento.getStato())
                .statoPrenotazione(
                        pagamento.getPrenotazione().getStato()
                )
                .dataPagamento(pagamento.getDataPagamento())
                .build();
    }
}