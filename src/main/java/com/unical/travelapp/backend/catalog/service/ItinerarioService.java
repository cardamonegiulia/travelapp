package com.unical.travelapp.backend.catalog.service;

import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.models.Immagine;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.experience.services.ImmagineService;
import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class ItinerarioService {

    @Autowired
    private ItinerarioRepository itinerarioRepository;

    @Autowired
    private DisponibilitaItinerarioRepository disponibilitaRepository;

    @Autowired
    private ImmagineService immagineService;

    @Autowired
    private ImmagineMapper immagineMapper;

    public Page<Itinerario> getAllItinerari(Pageable pageable) {
        return itinerarioRepository.findAll(pageable);
    }

    public Optional<Itinerario> getItinerarioById(Long id) {
        return itinerarioRepository.findById(id);
    }

    public List<DisponibilitaItinerario> getDisponibilitaByItinerarioId(Long itinerarioId) {
        if (!itinerarioRepository.existsById(itinerarioId)) {
            throw new ItinerarioNonTrovatoException("Itinerario non trovato: " + itinerarioId);
        }
        return disponibilitaRepository.findByItinerario_Id(itinerarioId);
    }

    @Transactional
    public Itinerario saveItinerario(Itinerario itinerario) {
        if (itinerario.getTappe() != null) {
            itinerario.getTappe().forEach(tappa -> tappa.setItinerario(itinerario));
        }
        return itinerarioRepository.save(itinerario);
    }

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

    @Transactional
    public void deleteItinerario(Long id, Utente richiedente, boolean isAdmin) {
        Itinerario itinerario = itinerarioModificabile(id, richiedente, isAdmin);

        immagineService.eliminaTutte(itinerario.getImmagini());
        itinerario.getImmagini().clear();

        if (itinerario.getRecensioni() != null) {
            for (Recensione recensione : itinerario.getRecensioni()) {
                immagineService.eliminaTutte(recensione.getImmagini());
                recensione.getImmagini().clear();
            }
        }

        itinerarioRepository.delete(itinerario);
    }

    // --- Immagini dell'itinerario ---------------------------------------------------------

    @Transactional(timeoutString = "${app.storage.immagini.upload-timeout-secondi:30}")
    public ImmagineResponse aggiungiImmagine(Long id, MultipartFile file, Utente richiedente, boolean isAdmin) {
        Itinerario itinerario = itinerarioModificabile(id, richiedente, isAdmin);
        immagineService.verificaLimite(itinerario.getImmagini().size());

        Immagine immagine = immagineService.caricaEntita(file);
        itinerario.getImmagini().add(immagine);
        itinerarioRepository.saveAndFlush(itinerario);

        return immagineMapper.toResponse(immagine);
    }

    public List<ImmagineResponse> getImmagini(Long id) {
        Itinerario itinerario = itinerarioRepository.findById(id)
                .orElseThrow(() -> new ItinerarioNonTrovatoException("Itinerario non trovato: " + id));
        return immagineMapper.toResponse(itinerario.getImmagini());
    }

    @Transactional
    public void rimuoviImmagine(Long id, Long immagineId, Utente richiedente, boolean isAdmin) {
        Itinerario itinerario = itinerarioModificabile(id, richiedente, isAdmin);

        Immagine immagine = itinerario.getImmagini().stream()
                .filter(i -> i.getId().equals(immagineId))
                .findFirst()
                .orElseThrow(() -> new ImmagineNonTrovata(
                        "Immagine non trovata sull'itinerario: " + immagineId));

        itinerario.getImmagini().remove(immagine);
        itinerarioRepository.saveAndFlush(itinerario);

        immagineService.eliminaEntita(immagine);
    }

    private Itinerario itinerarioModificabile(Long id, Utente richiedente, boolean isAdmin) {
        if (isAdmin) {
            return itinerarioRepository.findById(id)
                    .orElseThrow(() -> new ItinerarioNonTrovatoException("Itinerario non trovato: " + id));
        }

        return itinerarioRepository.findByIdAndOrganizzatore_Id(id, richiedente.getId())
                .orElseThrow(() -> new ItinerarioNonTrovatoException("Itinerario non trovato: " + id));
    }
}