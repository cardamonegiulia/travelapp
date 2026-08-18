package com.unical.travelapp.backend.catalog.service;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ItinerarioService {

    @Autowired
    private ItinerarioRepository itinerarioRepository;

    public Page<Itinerario> getAllItinerari(Pageable pageable) {
        return itinerarioRepository.findAll(pageable);
    }

    public Optional<Itinerario> getItinerarioById(Long id) {
        return itinerarioRepository.findById(id);
    }

    @Transactional
    public Itinerario saveItinerario(Itinerario itinerario) {
        if (itinerario.getTappe() != null) {
            itinerario.getTappe().forEach(tappa -> tappa.setItinerario(itinerario));
        }
        return itinerarioRepository.save(itinerario);
    }

    // ownership nella query: l'organizzatore puo' modificare solo i propri itinerari, l'admin qualsiasi
    @Transactional
    public Itinerario updateItinerario(Long id, Itinerario datiAggiornati, Utente richiedente, boolean isAdmin) {
        Itinerario esistente;

        if (isAdmin) {
            esistente = itinerarioRepository.findById(id)
                    .orElseThrow(() -> new ItinerarioNonTrovatoException("Itinerario non trovato: " + id));
        } else {
            esistente = itinerarioRepository.findByIdAndOrganizzatore_Id(id, richiedente.getId())
                    .orElseThrow(() -> new ItinerarioNonTrovatoException("Itinerario non trovato: " + id));
        }

        esistente.setTitolo(datiAggiornati.getTitolo());
        esistente.setDescrizione(datiAggiornati.getDescrizione());
        esistente.setDestinazionePrincipale(datiAggiornati.getDestinazionePrincipale());
        esistente.setPrezzoBase(datiAggiornati.getPrezzoBase());
        esistente.setDurataGiorni(datiAggiornati.getDurataGiorni());
        esistente.setMaxPartecipanti(datiAggiornati.getMaxPartecipanti());

        return itinerarioRepository.save(esistente);
    }

    // ownership nella query: l'organizzatore puo' cancellare solo i propri itinerari, l'admin qualsiasi
    @Transactional
    public void deleteItinerario(Long id, Utente richiedente, boolean isAdmin) {
        if (isAdmin) {
            if (!itinerarioRepository.existsById(id)) {
                throw new ItinerarioNonTrovatoException("Itinerario non trovato: " + id);
            }
            itinerarioRepository.deleteById(id);
            return;
        }

        Itinerario itinerario = itinerarioRepository.findByIdAndOrganizzatore_Id(id, richiedente.getId())
                .orElseThrow(() -> new ItinerarioNonTrovatoException("Itinerario non trovato: " + id));
        itinerarioRepository.delete(itinerario);
    }
}