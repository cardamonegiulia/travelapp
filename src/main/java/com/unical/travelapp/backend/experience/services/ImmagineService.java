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


    public record ContenutoImmagine(Resource risorsa, String contentType, long dimensioneByte, String nomeFile) {
    }

    public ImmagineResponse carica(MultipartFile file) {
        return mapper.toResponse(caricaEntita(file));
    }

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

            storage.elimina(archiviata.percorsoRelativo());
            throw e;
        }
    }

    public ImmagineResponse getById(Long id) {
        return mapper.toResponse(trovaImmagine(id));
    }


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

    public void elimina(Long id) {
        Immagine immagine = trovaImmagine(id);

        Utente utente = utenteService.getUtenteSessione();
        if (!immagine.getProprietario().getId().equals(utente.getId()) && !utenteService.isAdmin()) {

            throw new ImmagineNonTrovata("Immagine non trovata con id: " + id);
        }

        eliminaEntita(immagine);
    }


    public void eliminaEntita(Immagine immagine) {
        String percorso = immagine.getPercorsoRelativo();
        repo.delete(immagine);

        try {
            storage.elimina(percorso);
        } catch (RuntimeException e) {
            log.warn("Immagine {} rimossa dal database ma il file {} non e' stato cancellato",
                    immagine.getId(), percorso, e);
        }
    }


    public void eliminaTutte(Collection<Immagine> immagini) {
        if (immagini == null || immagini.isEmpty()) {
            return;
        }
        List.copyOf(immagini).forEach(this::eliminaEntita);
    }

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
