package com.unical.travelapp.backend.experience.mapper;

import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.models.Immagine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

// Conversione Immagine -> DTO. E' un componente a se' perche' serve anche fuori da
// experience: gli itinerari espongono le proprie immagini nel loro DTO e devono
// costruire gli URL nello stesso identico modo.
@Component
public class ImmagineMapper {

    private final String baseUrl;

    public ImmagineMapper(@Value("${app.storage.immagini.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ImmagineResponse toResponse(Immagine immagine) {
        if (immagine == null) {
            return null;
        }

        ImmagineResponse dto = new ImmagineResponse();
        dto.setId(immagine.getId());
        // al client va l'URL dell'endpoint, non il percorso sullo storage: e' un dettaglio
        // interno e cambierebbe il giorno in cui i file passano su un servizio esterno
        dto.setUrl(baseUrl + "/" + immagine.getId() + "/contenuto");
        dto.setContentType(immagine.getContentType());
        dto.setDimensioneByte(immagine.getDimensioneByte());
        dto.setLarghezza(immagine.getLarghezza());
        dto.setAltezza(immagine.getAltezza());
        dto.setProprietarioId(immagine.getProprietario().getId());
        dto.setCaricataIl(immagine.getCreatoIl());
        return dto;
    }

    // lista vuota e non null: al frontend arriva sempre un array su cui iterare
    public List<ImmagineResponse> toResponse(Collection<Immagine> immagini) {
        if (immagini == null) {
            return List.of();
        }
        return immagini.stream().map(this::toResponse).toList();
    }
}
