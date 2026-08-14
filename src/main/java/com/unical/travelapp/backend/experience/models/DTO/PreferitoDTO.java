package com.unical.travelapp.backend.experience.models.DTO;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import lombok.Data;

import java.util.List;

@Data
public class PreferitoDTO {

    private Long id;
    private Long utenteId;
    private List<ItinerarioDTO> itinerariList;
}
