package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonValida;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.models.Immagine;
import com.unical.travelapp.backend.experience.repository.ImmagineRepository;
import com.unical.travelapp.backend.experience.services.ImmagineStorageService.ImmagineArchiviata;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

/**
 * Orchestrazione dell'upload: identita' del chiamante, riga sul database, permessi.
 * La validazione del file e l'accesso al filesystem sono delegati per intero a
 * {@link ImmagineStorageService}.
 *
 * I metodi che finiscono in "Entita" sono pensati per gli altri service (recensioni,
 * itinerari) che allegano immagini alle proprie risorse: NON fanno controlli di permesso,
 * perche' chi li chiama ha gia' verificato di poter modificare la risorsa contenitore.
 */
@Service
public class ImmagineService {

    private static final Logger log = LoggerFactory.getLogger(ImmagineService.class);

    private final ImmagineRepository repo;
    private final ImmagineStorageService storage;
    private final ImmagineMapper mapper;
    private final UtenteService utenteService;
    private final int massimoPerRisorsa;

    public ImmagineService(ImmagineRepository repo,
                           ImmagineStorageService storage,
                           ImmagineMapper mapper,
                           UtenteService utenteService,
                           @Value("${app.storage.immagini.max-per-risorsa}") int massimoPerRisorsa) {
        this.repo = repo;
        this.storage = storage;
        this.mapper = mapper;
        this.utenteService = utenteService;
        this.massimoPerRisorsa = massimoPerRisorsa;
    }

    /** Contenuto binario di un'immagine, pronto per essere restituito dal controller. */
    public record ContenutoImmagine(Resource risorsa, String contentType, long dimensioneByte, String nomeFile) {
    }

    /** Carica un'immagine non ancora collegata ad alcuna risorsa. */
    public ImmagineResponse carica(MultipartFile file) {
        return mapper.toResponse(caricaEntita(file));
    }

    /**
     * Salva il file sullo storage e registra i metadati sul database.
     * Il proprietario e' sempre l'utente del token, mai un id passato dal client.
     */
    public Immagine caricaEntita(MultipartFile file) {

        Utente proprietario = utenteService.getUtenteSessione();

        ImmagineArchiviata archiviata = storage.salva(file);

        try {
            Immagine immagine = new Immagine();
            immagine.setPercorsoRelativo(archiviata.percorsoRelativo());
            immagine.setContentType(archiviata.contentType());
            immagine.setDimensioneByte(archiviata.dimensioneByte());
            immagine.setLarghezza(archiviata.larghezza());
            immagine.setAltezza(archiviata.altezza());
            immagine.setProprietario(proprietario);

            return repo.save(immagine);

        } catch (RuntimeException e) {
            // Il file e' gia' su disco ma la riga non e' stata scritta: senza questa pulizia
            // resterebbe un file che nessuno puo' piu' raggiungere ne' cancellare.
            storage.elimina(archiviata.percorsoRelativo());
            throw e;
        }
    }

    public ImmagineResponse getById(Long id) {
        return mapper.toResponse(trovaImmagine(id));
    }

    /** Immagini caricate dall'utente in sessione. */
    public Page<ImmagineResponse> getMie(Pageable pageable) {
        Utente utente = utenteService.getUtenteSessione();
        return repo.findByProprietario_Id(utente.getId(), pageable).map(mapper::toResponse);
    }

    public ContenutoImmagine getContenuto(Long id) {
        Immagine immagine = trovaImmagine(id);
        Resource risorsa = storage.carica(immagine.getPercorsoRelativo());

        return new ContenutoImmagine(risorsa, immagine.getContentType(),
                immagine.getDimensioneByte(), nomeFile(immagine));
    }

    /** Cancella metadati e file. Ammesso solo al proprietario e agli amministratori. */
    public void elimina(Long id) {
        Immagine immagine = trovaImmagine(id);

        Utente utente = utenteService.getUtenteSessione();
        if (!immagine.getProprietario().getId().equals(utente.getId()) && !utenteService.isAdmin()) {
            // 404 e non 403: rispondere "non tua" confermerebbe a chi sonda gli id che
            // l'immagine esiste (convenzione del progetto, vedi docs/SECURITY.md)
            throw new ImmagineNonTrovata("Immagine non trovata con id: " + id);
        }

        eliminaEntita(immagine);
    }

    /**
     * Cancella riga e file senza controlli di permesso: la usano i service che gestiscono
     * la risorsa a cui l'immagine e' allegata, dopo aver verificato i propri.
     */
    public void eliminaEntita(Immagine immagine) {
        String percorso = immagine.getPercorsoRelativo();
        repo.delete(immagine);

        try {
            storage.elimina(percorso);
        } catch (RuntimeException e) {
            // La riga e' gia' sparita: per il client l'operazione e' riuscita. Il file
            // rimasto sullo storage e' spazzatura da ripulire, non un errore da propagare.
            log.warn("Immagine {} rimossa dal database ma il file {} non e' stato cancellato",
                    immagine.getId(), percorso, e);
        }
    }

    /**
     * Cancella un gruppo di immagini: serve quando sparisce la risorsa che le contiene
     * (recensione o itinerario), altrimenti i file resterebbero sullo storage per sempre.
     */
    public void eliminaTutte(Collection<Immagine> immagini) {
        if (immagini == null || immagini.isEmpty()) {
            return;
        }
        // copia: i chiamanti passano la collection gestita da Hibernate, che non va
        // modificata mentre la si sta percorrendo
        List.copyOf(immagini).forEach(this::eliminaEntita);
    }

    /**
     * Verifica che ci sia ancora posto per un'altra immagine su una risorsa.
     * Senza un tetto, un solo utente potrebbe allegare immagini a una sua recensione
     * all'infinito e riempire lo storage restando dentro il limite del singolo file.
     */
    public void verificaLimite(int immaginiGiaPresenti) {
        if (immaginiGiaPresenti >= massimoPerRisorsa) {
            throw new ImmagineNonValida(
                    "Numero massimo di immagini raggiunto: " + massimoPerRisorsa);
        }
    }

    private Immagine trovaImmagine(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ImmagineNonTrovata("Immagine non trovata con id: " + id));
    }

    private String nomeFile(Immagine immagine) {
        String percorso = immagine.getPercorsoRelativo();
        return percorso.substring(percorso.lastIndexOf('/') + 1);
    }
}
