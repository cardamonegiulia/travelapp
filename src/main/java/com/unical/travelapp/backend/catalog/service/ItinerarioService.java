package com.unical.travelapp.backend.catalog.service;

import com.unical.travelapp.backend.catalog.dto.AttivitaExtraDTO;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.GiornoProgramma;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    @Autowired
    private AttivitaRepository attivitaRepository;

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

    public List<AttivitaExtraDTO> getAttivitaExtra(Long itinerarioId) {

        if (!itinerarioRepository.existsById(itinerarioId)) {
            throw new ItinerarioNonTrovatoException(
                    "Itinerario non trovato: " + itinerarioId
            );
        }

        return attivitaRepository
                .findExtraByItinerarioId(itinerarioId)
                .stream()
                .map(attivita ->
                        new AttivitaExtraDTO(
                                attivita.getId(),
                                attivita.getTitolo(),
                                attivita.getPrezzoExtra()
                        )
                )
                .toList();
    }

    @Transactional
    public Itinerario saveItinerario(Itinerario itinerario) {
        if (itinerario.getTappe() != null) {
            itinerario.getTappe().forEach(tappa -> tappa.setItinerario(itinerario));
        }
        if (itinerario.getDisponibilita() != null) {
            itinerario.getDisponibilita().forEach(periodo -> periodo.setItinerario(itinerario));
        }
        if (itinerario.getProgramma() != null) {
            itinerario.getProgramma().forEach(giorno -> giorno.setItinerario(itinerario));
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

        sostituisciProgramma(esistente, datiAggiornati.getProgramma());
        aggiungiPartenza(esistente, datiAggiornati.getDisponibilita());

        return itinerarioRepository.save(esistente);
    }

    // Il programma, a differenza delle partenze, si riscrive per intero a ogni salvataggio:
    // e' la descrizione del viaggio, non un impegno preso con chi ha gia' prenotato.
    // La lista esistente viene svuotata invece di essere sostituita, perche' e' orphanRemoval:
    // solo cosi' Hibernate si accorge delle giornate rimosse e le cancella davvero.
    private void sostituisciProgramma(Itinerario esistente, List<GiornoProgramma> nuovoProgramma) {
        if (nuovoProgramma == null) {
            return;
        }

        if (esistente.getProgramma() == null) {
            esistente.setProgramma(new ArrayList<>());
        }

        esistente.getProgramma().clear();

        for (GiornoProgramma giorno : nuovoProgramma) {
            giorno.setItinerario(esistente);
            esistente.getProgramma().add(giorno);
        }
    }

    // Il periodo che arriva con un aggiornamento e' sempre una partenza in piu': le date
    // gia' pubblicate non si spostano, perche' chi ha prenotato si e' impegnato su quelle e
    // spostargliele sotto i piedi sarebbe un altro viaggio. Per togliere una partenza c'e'
    // l'eliminazione esplicita, che si rifiuta di cancellare quelle gia' vendute.
    //
    // Un periodo identico a uno gia' presente non viene aggiunto: cosi' salvare due volte
    // lo stesso form non lascia doppioni.
    private void aggiungiPartenza(Itinerario esistente, List<DisponibilitaItinerario> periodoRichiesto) {
        if (periodoRichiesto == null || periodoRichiesto.isEmpty()) {
            return;
        }

        DisponibilitaItinerario richiesto = periodoRichiesto.get(0);

        if (esistente.getDisponibilita() == null) {
            esistente.setDisponibilita(new ArrayList<>());
        }
        List<DisponibilitaItinerario> attuali = esistente.getDisponibilita();

        boolean giaPresente = attuali.stream().anyMatch(d ->
                Objects.equals(d.getDataInizio(), richiesto.getDataInizio())
                        && Objects.equals(d.getDataFine(), richiesto.getDataFine()));

        if (giaPresente) {
            return;
        }

        richiesto.setItinerario(esistente);
        attuali.add(richiesto);
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