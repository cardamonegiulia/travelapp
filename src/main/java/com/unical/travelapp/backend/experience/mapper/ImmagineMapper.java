package com.unical.travelapp.backend.experience.mapper;

import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.models.Immagine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

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
        dto.setUrl(baseUrl + "/" + immagine.getId() + "/contenuto");
        dto.setContentType(immagine.getContentType());
        dto.setDimensioneByte(immagine.getDimensioneByte());
        dto.setLarghezza(immagine.getLarghezza());
        dto.setAltezza(immagine.getAltezza());
        dto.setProprietarioId(immagine.getProprietario().getId());
        dto.setCaricataIl(immagine.getCreatoIl());
        return dto;
    }

    public List<ImmagineResponse> toResponse(Collection<Immagine> immagini) {
        if (immagini == null) {
            return List.of();
        }
        return immagini.stream().map(this::toResponse).toList();
    }
}
