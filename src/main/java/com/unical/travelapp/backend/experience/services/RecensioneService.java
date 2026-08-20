package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import com.unical.travelapp.backend.experience.exeption.ItinerarioNonTrovato;
import com.unical.travelapp.backend.experience.exeption.PrenotazioneNonTrovata;
import com.unical.travelapp.backend.experience.exeption.RecensioneNonTrovata;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneRequest;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneResponse;
import com.unical.travelapp.backend.experience.models.Immagine;
import com.unical.travelapp.backend.experience.models.Recensione;
import com.unical.travelapp.backend.experience.repository.RecensioneRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class RecensioneService {

    @Autowired
    private RecensioneRepository repo;

    @Autowired
    private UtenteService utenteService;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    @Autowired
    private ItinerarioRepository itinerarioRepository;

    @Autowired
    private ImmagineService immagineService;

    @Autowired
    private ImmagineMapper immagineMapper;


    public RecensioneResponse getById(Long id) {
        Recensione recensione = repo.findById(id)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata con id: " + id));
        return toResponse(recensione);
    }


    public Page<RecensioneResponse> getRecensioniDaItinerarioId(Long itinerarioId, Pageable pageable) {
        return repo.findByItinerario_Id(itinerarioId, pageable).map(this::toResponse);
    }


    public double getMediaVoti(Long itinerarioId) {
        List<Recensione> recensioni = repo.findByItinerario_Id(itinerarioId);
        return recensioni.stream()
                .mapToInt(Recensione::getVoto)
                .average()
                .orElse(0.0);
    }


    public Long addNewRecensione(RecensioneRequest dto) {

        Utente utente = utenteService.getUtenteSessione();

        // Ricava l'itinerario: prima dal campo diretto, poi dalla prenotazione
        Itinerario itinerario = resolveItinerario(dto);

        // Gestione prenotazione (opzionale)
        Prenotazione prenotazione = null;
        if (dto.getPrenotazioneId() != null) {
            prenotazione = prenotazioneRepository
                    .findById(dto.getPrenotazioneId())
                    .orElseThrow(() -> new PrenotazioneNonTrovata("Prenotazione non trovata"));

            if (!prenotazione.getViaggiatore().getId().equals(utente.getId())) {
                throw new AccessDeniedException("Non autorizzato a recensire questa prenotazione");
            }

            if (repo.existsByPrenotazione(prenotazione)) {
                throw new IllegalStateException("Hai gia' recensito questa prenotazione");
            }
        }

        Recensione recensione = new Recensione();
        recensione.setUtente(utente);
        recensione.setPrenotazione(prenotazione);
        recensione.setItinerario(itinerario);
        recensione.setCommento(dto.getComm());
        recensione.setVoto(dto.getVotazione());

        return repo.save(recensione).getId();
    }



    @Transactional
    public void deleteRecensione(Long id) {
        Recensione recensione = repo.findById(id)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata"));

        Utente utente = utenteService.getUtenteSessione();
        if (!recensione.getUtente().getId().equals(utente.getId())) {
            throw new AccessDeniedException("Non autorizzato a eliminare questa recensione");
        }

        // prima le foto: se sparisse solo la recensione, i file resterebbero sullo storage
        // senza che nessuno possa piu' raggiungerli per cancellarli
        immagineService.eliminaTutte(recensione.getImmagini());
        recensione.getImmagini().clear();

        repo.delete(recensione);
    }


    // --- Foto allegate alla recensione ---------------------------------------------------

    /** Allega una foto alla recensione. Consentito all'autore e, per moderazione, agli admin. */
    @Transactional
    public ImmagineResponse aggiungiImmagine(Long recensioneId, MultipartFile file) {
        Recensione recensione = recensioneModificabile(recensioneId);
        immagineService.verificaLimite(recensione.getImmagini().size());

        Immagine immagine = immagineService.caricaEntita(file);
        recensione.getImmagini().add(immagine);
        repo.save(recensione);

        return immagineMapper.toResponse(immagine);
    }


    public List<ImmagineResponse> getImmagini(Long recensioneId) {
        Recensione recensione = repo.findById(recensioneId)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata con id: " + recensioneId));
        return immagineMapper.toResponse(recensione.getImmagini());
    }


    @Transactional
    public void rimuoviImmagine(Long recensioneId, Long immagineId) {
        Recensione recensione = recensioneModificabile(recensioneId);

        // l'immagine deve appartenere proprio a questa recensione: senza questo controllo
        // l'autore di una recensione potrebbe cancellare le foto di quelle altrui
        Immagine immagine = recensione.getImmagini().stream()
                .filter(i -> i.getId().equals(immagineId))
                .findFirst()
                .orElseThrow(() -> new ImmagineNonTrovata(
                        "Immagine non trovata sulla recensione: " + immagineId));

        recensione.getImmagini().remove(immagine);
        repo.save(recensione);

        immagineService.eliminaEntita(immagine);
    }


    // Recensione su cui il chiamante puo' intervenire. Come deleteRecensione risponde 403 e
    // non 404: l'id della recensione e' pubblico (compare nell'elenco di un itinerario),
    // quindi qui non c'e' nessuna esistenza da nascondere.
    private Recensione recensioneModificabile(Long id) {
        Recensione recensione = repo.findById(id)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata con id: " + id));

        Utente utente = utenteService.getUtenteSessione();
        if (!recensione.getUtente().getId().equals(utente.getId()) && !utenteService.isAdmin()) {
            throw new AccessDeniedException("Non autorizzato a modificare questa recensione");
        }

        return recensione;
    }


    private RecensioneResponse toResponse(Recensione r) {
        RecensioneResponse dto = new RecensioneResponse();
        dto.setId(r.getId());
        dto.setUtenteId(r.getUtente().getId());
        dto.setComm(r.getCommento());
        dto.setVotazione(r.getVoto());
        dto.setImmagini(immagineMapper.toResponse(r.getImmagini()));

        if (r.getItinerario() != null) {
            dto.setItinerarioId(r.getItinerario().getId());
        }
        if (r.getPrenotazione() != null) {
            dto.setPrenotazioneId(r.getPrenotazione().getId());
        }
        return dto;
    }


    private Itinerario resolveItinerario(RecensioneRequest dto) {
        if (dto.getItinerarioId() != null) {
            return itinerarioRepository.findById(dto.getItinerarioId())
                    .orElseThrow(() -> new ItinerarioNonTrovato("Itinerario non trovato"));
        }

        if (dto.getPrenotazioneId() != null) {
            Optional<Prenotazione> prenotazione = prenotazioneRepository.findById(dto.getPrenotazioneId());
            if (prenotazione.isPresent() && prenotazione.get().getDisponibilitaItinerario() != null) {
                return prenotazione.get().getDisponibilitaItinerario().getItinerario();
            }
        }

        throw new IllegalArgumentException("Fornire itinerarioId oppure prenotazioneId valido con disponibilita'");
    }
}
