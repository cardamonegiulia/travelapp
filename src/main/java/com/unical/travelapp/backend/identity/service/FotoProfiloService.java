package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.experience.models.Immagine;
import com.unical.travelapp.backend.experience.services.ImmagineService;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.mapper.UtenteMapper;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotoProfiloService {

    private final UtenteRepository utenteRepository;
    private final UtenteService utenteService;
    private final ImmagineService immagineService;
    private final UtenteMapper utenteMapper;

    public FotoProfiloService(UtenteRepository utenteRepository,
                              UtenteService utenteService,
                              ImmagineService immagineService,
                              UtenteMapper utenteMapper) {
        this.utenteRepository = utenteRepository;
        this.utenteService = utenteService;
        this.immagineService = immagineService;
        this.utenteMapper = utenteMapper;
    }

    @Transactional(timeoutString = "${app.storage.immagini.upload-timeout-secondi}")
    public UtenteResponseDto imposta(MultipartFile file) {
        Utente utente = utenteService.getUtenteSessione();
        Immagine precedente = utente.getFotoProfilo();

        utente.setFotoProfilo(immagineService.caricaEntita(file));
        Utente aggiornato = utenteRepository.saveAndFlush(utente);

        if (precedente != null) {
            immagineService.eliminaEntita(precedente);
        }
        return utenteMapper.toResponseDto(aggiornato);
    }

    @Transactional
    public void rimuovi() {
        Utente utente = utenteService.getUtenteSessione();
        Immagine precedente = utente.getFotoProfilo();
        if (precedente == null) {
            return;
        }

        utente.setFotoProfilo(null);
        utenteRepository.saveAndFlush(utente);
        immagineService.eliminaEntita(precedente);
    }
}
