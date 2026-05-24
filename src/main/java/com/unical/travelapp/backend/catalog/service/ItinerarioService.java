package com.unical.travelapp.backend.catalog.service;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.catalog.exception.RisorsaNonTrovataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItinerarioService {

    @Autowired
    private ItinerarioRepository itinerarioRepository;


    public List<Itinerario> getAllItinerari() {
        return itinerarioRepository.findAll();
    }

    public Itinerario getItinerarioById(Long id) {
        return itinerarioRepository.findById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Itinerario con ID " + id + " non trovato nel catalogo"));
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
        if (!itinerarioRepository.existsById(id)) {
            throw new RisorsaNonTrovataException("Impossibile eliminare: Itinerario con ID " + id + " non esiste");
        }
        itinerarioRepository.deleteById(id);
    }

}