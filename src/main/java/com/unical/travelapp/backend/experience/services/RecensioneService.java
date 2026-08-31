package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import com.unical.travelapp.backend.experience.exeption.PrenotazioneNonTrovata;
import com.unical.travelapp.backend.experience.exeption.RecensioneNonTrovata;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import com.unical.travelapp.backend.experience.models.DTO.AggiornaRecensioneRequest;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneRequest;
import com.unical.travelapp.backend.experience.models.DTO.RecensioneResponse;
import com.unical.travelapp.backend.experience.models.DTO.ValutazioneMediaDTO;
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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecensioneService {

    @Autowired
    private RecensioneRepository repo;

    @Autowired
    private UtenteService utenteService;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    @Autowired
    private ImmagineService immagineService;

    @Autowired
    private ImmagineMapper immagineMapper;

    @Autowired
    private NotificaService notificaService;


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


    /**
     * Media e conteggio per un gruppo di itinerari, in una sola query.
     *
     * <p>Gli itinerari senza recensioni sono comunque presenti nella mappa, con
     * {@link ValutazioneMediaDTO#NESSUNA}: chi la usa non deve gestire il caso "chiave
     * assente" per distinguere "nessun voto" da "itinerario sconosciuto".
     */
    public Map<Long, ValutazioneMediaDTO> getValutazioni(Collection<Long> itinerarioIds) {
        Map<Long, ValutazioneMediaDTO> valutazioni = new HashMap<>();
        if (itinerarioIds == null || itinerarioIds.isEmpty()) {
            return valutazioni;
        }

        itinerarioIds.forEach(id -> valutazioni.put(id, ValutazioneMediaDTO.NESSUNA));

        repo.statistichePerItinerari(itinerarioIds).forEach(statistica ->
                valutazioni.put(statistica.getItinerarioId(),
                        new ValutazioneMediaDTO(statistica.getMedia(),
                                statistica.getNumero() == null ? 0L : statistica.getNumero())));

        return valutazioni;
    }


    public ValutazioneMediaDTO getValutazione(Long itinerarioId) {
        return getValutazioni(List.of(itinerarioId)).getOrDefault(itinerarioId, ValutazioneMediaDTO.NESSUNA);
    }


    /** Id della recensione gia' scritta, per ciascuna delle prenotazioni indicate. */
    public Map<Long, Long> getRecensioniPerPrenotazioni(Collection<Long> prenotazioneIds) {
        if (prenotazioneIds == null || prenotazioneIds.isEmpty()) {
            return Map.of();
        }
        return repo.findByPrenotazione_IdIn(prenotazioneIds).stream()
                .collect(Collectors.toMap(r -> r.getPrenotazione().getId(), Recensione::getId,
                        (primo, secondo) -> primo));
    }


    /** La recensione lasciata su una prenotazione, visibile solo a chi l'ha prenotata (o a un ADMIN). */
    public Optional<RecensioneResponse> getRecensionePerPrenotazione(Long prenotazioneId) {
        Prenotazione prenotazione = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNonTrovata("Prenotazione non trovata"));

        verificaProprietaPrenotazione(prenotazione);

        return repo.findByPrenotazione_Id(prenotazioneId).map(this::toResponse);
    }


    @Transactional
    public Long addNewRecensione(RecensioneRequest dto) {

        Utente utente = utenteService.getUtenteSessione();

        Prenotazione prenotazione = prenotazioneRepository
                .findById(dto.getPrenotazioneId())
                .orElseThrow(() -> new PrenotazioneNonTrovata("Prenotazione non trovata"));

        // 1. solo chi ha prenotato quel viaggio puo' recensirlo
        if (!prenotazione.getViaggiatore().getId().equals(utente.getId())) {
            throw new AccessDeniedException("Non autorizzato a recensire questa prenotazione");
        }

        // 2. e solo dopo che il viaggio si e' concluso
        verificaViaggioConcluso(prenotazione);

        // 3. una sola recensione per prenotazione: la seconda si fa modificando la prima
        if (repo.existsByPrenotazione(prenotazione)) {
            throw new IllegalStateException("Hai gia' recensito questa prenotazione");
        }

        Itinerario itinerario = itinerarioDellaPrenotazione(prenotazione);

        // L'itinerario nel payload e' facoltativo, ma se c'e' deve essere quello giusto:
        // altrimenti la recensione di un viaggio finirebbe sotto un itinerario diverso.
        if (dto.getItinerarioId() != null && !dto.getItinerarioId().equals(itinerario.getId())) {
            throw new IllegalArgumentException("L'itinerario indicato non corrisponde alla prenotazione");
        }

        Recensione recensione = new Recensione();
        recensione.setUtente(utente);
        recensione.setPrenotazione(prenotazione);
        recensione.setItinerario(itinerario);
        recensione.setCommento(dto.getComm());
        recensione.setVoto(dto.getVotazione());

        Long id = repo.save(recensione).getId();

        // L'invito a recensire ha esaurito il suo scopo: non deve restare fra le notifiche
        notificaService.rimuoviInvitoRecensione(prenotazione.getId());

        return id;
    }


    /**
     * Modifica voto e commento di una recensione gia' scritta.
     *
     * <p>Solo l'autore: e' la sua opinione. Un ADMIN puo' cancellarla (moderazione), non
     * riscriverla.
     */
    @Transactional
    public RecensioneResponse aggiornaRecensione(Long id, AggiornaRecensioneRequest dto) {
        Recensione recensione = repo.findById(id)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata con id: " + id));

        Utente utente = utenteService.getUtenteSessione();
        if (!recensione.getUtente().getId().equals(utente.getId())) {
            throw new AccessDeniedException("Non autorizzato a modificare questa recensione");
        }

        recensione.setVoto(dto.getVotazione());
        recensione.setCommento(dto.getComm());

        return toResponse(repo.save(recensione));
    }


    @Transactional
    public void deleteRecensione(Long id) {
        Recensione recensione = repo.findById(id)
                .orElseThrow(() -> new RecensioneNonTrovata("Recensione non trovata"));

        Utente utente = utenteService.getUtenteSessione();
        if (!recensione.getUtente().getId().equals(utente.getId()) && !utenteService.isAdmin()) {
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


    // --- Regole condivise ----------------------------------------------------------------

    /**
     * Vero quando il viaggio prenotato e' finito: la data di fine (della partenza scelta o
     * della sessione) e' passata e la prenotazione non e' stata cancellata.
     */
    public static boolean viaggioConcluso(Prenotazione prenotazione, LocalDateTime adesso) {
        if (prenotazione.getStato() == StatoPrenotazione.CANCELLATA) {
            return false;
        }
        LocalDateTime fine = dataFine(prenotazione);
        return fine != null && fine.isBefore(adesso);
    }

    /** Data di fine del viaggio prenotato, qualunque sia il tipo di prenotazione. */
    public static LocalDateTime dataFine(Prenotazione prenotazione) {
        if (prenotazione.getDisponibilitaItinerario() != null) {
            return prenotazione.getDisponibilitaItinerario().getDataFine();
        }
        if (prenotazione.getSessioneSingolaAttivita() != null) {
            return prenotazione.getSessioneSingolaAttivita().getDataFine();
        }
        return null;
    }

    /** Itinerario di una prenotazione, se ne ha uno (le attivita' singole non lo hanno). */
    public static Optional<Itinerario> itinerarioDi(Prenotazione prenotazione) {
        return Optional.ofNullable(prenotazione.getDisponibilitaItinerario())
                .map(disponibilita -> disponibilita.getItinerario());
    }


    private void verificaViaggioConcluso(Prenotazione prenotazione) {
        if (prenotazione.getStato() == StatoPrenotazione.CANCELLATA) {
            throw new IllegalStateException("Una prenotazione cancellata non puo' essere recensita");
        }
        if (!viaggioConcluso(prenotazione, LocalDateTime.now())) {
            throw new IllegalStateException("Puoi recensire il viaggio solo dopo che si e' concluso");
        }
    }


    private Itinerario itinerarioDellaPrenotazione(Prenotazione prenotazione) {
        return itinerarioDi(prenotazione).orElseThrow(() -> new IllegalArgumentException(
                "Questa prenotazione non e' collegata a un itinerario recensibile"));
    }


    private void verificaProprietaPrenotazione(Prenotazione prenotazione) {
        if (utenteService.isAdmin()) {
            return;
        }
        Utente utente = utenteService.getUtenteSessione();
        if (!prenotazione.getViaggiatore().getId().equals(utente.getId())) {
            throw new AccessDeniedException("Non autorizzato a consultare questa prenotazione");
        }
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
        dto.setAutoreNome(r.getUtente().getNome());
        dto.setAutoreCognome(r.getUtente().getCognome());
        dto.setComm(r.getCommento());
        dto.setVotazione(r.getVoto());
        dto.setDataRecensione(r.getCreatoIl());
        dto.setImmagini(immagineMapper.toResponse(r.getImmagini()));

        if (r.getItinerario() != null) {
            dto.setItinerarioId(r.getItinerario().getId());
        }
        if (r.getPrenotazione() != null) {
            dto.setPrenotazioneId(r.getPrenotazione().getId());
        }
        return dto;
    }
}
