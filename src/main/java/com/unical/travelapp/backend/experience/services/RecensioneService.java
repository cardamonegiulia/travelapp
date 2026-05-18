package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.experience.models.DTO.RecensioneDTO;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.experience.repository.RecensioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RecensioneService {

    @Autowired
    private RecensioneRepository repo;

    public RecensioneDTO getById(Long id){
        RecensioneDTO dto = null;

        Optional<Recensione> recensione = repo.findById(id);

        try {
            if (recensione != null) {
                dto = new RecensioneDTO();
                dto.setPreno(recensione.get().getPrenotazione());
                dto.setUt(recensione.get().getUtente());
                dto.setComm(recensione.get().getCommento());
                dto.setVotazione(recensione.get().getVoto());
            }
        }
        catch (Exception e){
            System.out.println(e);
        }

        return dto;
    }
}
