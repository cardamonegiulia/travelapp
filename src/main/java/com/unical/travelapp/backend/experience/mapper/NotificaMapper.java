package com.unical.travelapp.backend.experience.mapper;

import com.unical.travelapp.backend.experience.models.DTO.NotificaResponse;
import com.unical.travelapp.backend.experience.models.Notifica;
import org.springframework.stereotype.Component;

@Component
public class NotificaMapper {

    public NotificaResponse toResponse(Notifica notifica) {
        if (notifica == null) return null;

        NotificaResponse dto = new NotificaResponse();
        dto.setId(notifica.getId());
        dto.setTipo(notifica.getTipo());
        dto.setTitolo(notifica.getTitolo());
        dto.setMessaggio(notifica.getMessaggio());
        dto.setLetta(notifica.isLetta());
        dto.setDataCreazione(notifica.getCreatoIl());

        if (notifica.getPrenotazione() != null) {
            dto.setPrenotazioneId(notifica.getPrenotazione().getId());
        }
        if (notifica.getItinerario() != null) {
            dto.setItinerarioId(notifica.getItinerario().getId());
            dto.setTitoloViaggio(notifica.getItinerario().getTitolo());
        }
        return dto;
    }
}
