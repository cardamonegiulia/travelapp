package com.unical.travelapp.backend.experience.models.DTO;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Utente;
import lombok.Data;

import java.util.List;

@Data
public class PreferitoDTO {

    private Long id;
    private Utente utente;
    private List<Itinerario> itinerarioList;
}
