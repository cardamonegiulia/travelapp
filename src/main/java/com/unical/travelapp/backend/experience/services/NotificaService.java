package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.exeption.NotificaNonTrovata;
import com.unical.travelapp.backend.experience.mapper.NotificaMapper;
import com.unical.travelapp.backend.experience.models.DTO.NotificaResponse;
import com.unical.travelapp.backend.experience.models.Notifica;
import com.unical.travelapp.backend.experience.models.TipoNotifica;
import com.unical.travelapp.backend.experience.repository.NotificaRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificaService {

    private final NotificaRepository repo;
    private final NotificaMapper mapper;
    private final UtenteService utenteService;

    public NotificaService(NotificaRepository repo, NotificaMapper mapper, UtenteService utenteService) {
        this.repo = repo;
        this.mapper = mapper;
        this.utenteService = utenteService;
    }

    public Page<NotificaResponse> getMieNotifiche(Pageable pageable) {
        Long destinatarioId = utenteService.getUtenteSessione().getId();
        return repo.findByDestinatario_IdOrderByIdDesc(destinatarioId, pageable).map(mapper::toResponse);
    }

    public long contaMieNonLette() {
        return repo.countByDestinatario_IdAndLettaFalse(utenteService.getUtenteSessione().getId());
    }

    @Transactional
    public NotificaResponse segnaLetta(Long id) {
        Long destinatarioId = utenteService.getUtenteSessione().getId();

        Notifica notifica = repo.findByIdAndDestinatario_Id(id, destinatarioId)
                .orElseThrow(() -> new NotificaNonTrovata("Notifica non trovata: " + id));

        notifica.setLetta(true);
        return mapper.toResponse(repo.save(notifica));
    }


    @Transactional
    public boolean creaInvitoRecensione(Prenotazione prenotazione, Itinerario itinerario) {
        if (repo.existsByPrenotazione_IdAndTipo(prenotazione.getId(), TipoNotifica.INVITO_RECENSIONE)) {
            return false;
        }

        Utente destinatario = prenotazione.getViaggiatore();
        String titoloViaggio = itinerario != null ? itinerario.getTitolo() : "il tuo viaggio";

        Notifica notifica = new Notifica();
        notifica.setDestinatario(destinatario);
        notifica.setTipo(TipoNotifica.INVITO_RECENSIONE);
        notifica.setTitolo("Com'e' andata?");
        notifica.setMessaggio("Il tuo viaggio \"" + titoloViaggio
                + "\" si e' concluso: lascia una recensione e aiuta gli altri viaggiatori.");
        notifica.setPrenotazione(prenotazione);
        notifica.setItinerario(itinerario);

        repo.save(notifica);
        return true;
    }


    @Transactional
    public void rimuoviInvitoRecensione(Long prenotazioneId) {
        List<Notifica> inviti = repo.findByPrenotazione_IdAndTipo(prenotazioneId, TipoNotifica.INVITO_RECENSIONE);
        if (!inviti.isEmpty()) {
            repo.deleteAll(inviti);
        }
    }
}
