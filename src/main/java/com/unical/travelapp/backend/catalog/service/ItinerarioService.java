package com.unical.travelapp.backend.catalog.service;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ItinerarioService {

    @Autowired
    private ItinerarioRepository itinerarioRepository;

   
    public List<Itinerario> getAllItinerari() {
        return itinerarioRepository.findAll();
    }


    public Optional<Itinerario> getItinerarioById(Long id) {
        return itinerarioRepository.findById(id);
    }

    @Transactional //importante x l'integrità dei dati
    public Itinerario saveItinerario(Itinerario itinerario) {
        if (itinerario.getTappe() != null) {
            itinerario.getTappe().forEach(tappa -> tappa.setItinerario(itinerario));
        }
        return itinerarioRepository.save(itinerario);
    }


    @Transactional
    public void deleteItinerario(Long id) {
        itinerarioRepository.deleteById(id);
    }

}