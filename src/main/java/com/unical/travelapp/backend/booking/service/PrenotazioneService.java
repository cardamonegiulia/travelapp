package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.dto.CrePrenotazioneRequest;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class PrenotazioneService {
    // qui mettero dentro i repository per parlare con il DB

    private final PrenotazioneRepository prenotazioneRepo;
    private final ExtraPrenotazioneRepository extraPrenotazioneRepo;
    private final PagamentoRepository pagamentoRepo;

    //private final UtenteRepository utenteRepository;
    //private final DisponibilitaItinerarioRepository disponibilitaItinerarioRepository;
    //private final SessioneSingolaAttivitaRepository sessioneSingolaAttivitaRepository;

    // primo metodo mongolo, giusto per iniziare
    public List<Prenotazione> getAllPrenotazioni() {
        return prenotazioneRepo.findAll();
    }

    @Transactional
    public Prenotazione createPrenotazione(CrePrenotazioneRequest req) {
        //partiamo da un controllo base, ovvero utente o sceglie un viaggio o lun it, senno errore
        if (req.getNumeroPartecipanti() == null || req.getNumeroPartecipanti() <= 0) {
            throw new IllegalArgumentException("Numero partecipanti non valido");
        }

        boolean isItinerario = req.getDisponibilitaItinerarioId() != null;
        boolean isSessione = req.getSessioneSingolaAttivitaId() != null;

        if (isItinerario == isSessione) {
            throw new IllegalArgumentException("Devi scegliare o un itinerario o una singola attività");
        }
        //momentaneo
        return null;
    }
}
