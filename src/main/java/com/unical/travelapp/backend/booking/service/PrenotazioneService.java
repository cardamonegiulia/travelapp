package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.dto.CrePrenotazioneRequest;
import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class PrenotazioneService {
    // qui mettero dentro i repository per parlare con il DB

    private final PrenotazioneRepository prenotazioneRepo;
    private final ExtraPrenotazioneRepository extraPrenotazioneRepo;
    private final PagamentoRepository pagamentoRepo;


    //private final UtenteRepository utenteRepository;
    private final DisponibilitaItinerarioRepository disponibilitaItinerarioRepository;
    private final SessioneSingolaAttivitaRepository sessioneSingolaAttivitaRepository;


    // cretePrenotazione era diventata troppo grande quindi ora creo diversi piccoli metodi
    // cosi da avere una logica pulita e leggibile da usare poi dentro createPrenotazione.

    private void validaRichiesta(CrePrenotazioneRequest req){
        if(req.getNumeroPartecipanti() == null || req.getNumeroPartecipanti() <= 0){
            throw new IllegalArgumentException  ("numeri posti non disponibili");
        }
        if(req.getDisponibilitaItinerarioId() == null && req.getSessioneSingolaAttivitaId() == null ){
            throw new IllegalArgumentException ("Devi selezionare un itinerario o una singola attività");
        }
        if(req.getDisponibilitaItinerarioId() != null && req.getSessioneSingolaAttivitaId() != null ){
            throw new IllegalArgumentException ("Devi selezionare un itinerario o una singola attività");
        }
    }
    private DisponibilitaItinerario recuperaDisponibilita (Long id){
        Optional<DisponibilitaItinerario> dispon = disponibilitaItinerarioRepository.findById(id);
        if(!dispon.isPresent()){
            throw new IllegalArgumentException("Itinerario non trovato");
        }
        return dispon.get();

    }
    private SessioneSingolaAttivita recuperaSingolaAttivita (Long id){
        Optional<SessioneSingolaAttivita> sessioneSing = sessioneSingolaAttivitaRepository.findById(id);
        if(!sessioneSing.isPresent()){
            throw new IllegalArgumentException("Sessione non trovata");
        }
        return sessioneSing.get();
    }

    // piu avanti evito la duplicazioni dei due metodi scalaposti.
    private void controllaEScalaPostiSessione(SessioneSingolaAttivita sessione, Integer numeroPartecipanti){
        if (sessione.getPostiDisponibili()<numeroPartecipanti ){
            throw new IllegalArgumentException("Numero posti non disponibili");
        }
        sessione.setPostiDisponibili(sessione.getPostiDisponibili() - numeroPartecipanti);

    }
    private void controllaEScalaPostiItinerario(DisponibilitaItinerario disp, Integer numeroPartecipanti){
        if (disp.getPostiDisponibili()<numeroPartecipanti ){
            throw new IllegalArgumentException("Numero posti non disponibili");
        }
        disp.setPostiDisponibili(disp.getPostiDisponibili() - numeroPartecipanti);
    }

    // Lo compatto appena capisco e cerco poi di evitare i duplicati come con scala posto. Sorry guys.
    private BigDecimal calcolaPrezzoItinerario(DisponibilitaItinerario disp, Integer numeroPartecipanti) {
        BigDecimal prezzoBase = disp.getItinerario().getPrezzoBase();
        BigDecimal partecipanti = BigDecimal.valueOf(numeroPartecipanti); // converto Big in un inter int se non erro
        // cosi posso moltiplicare (dannato Bigdecimal)
        return prezzoBase.multiply(partecipanti);
    }

    private BigDecimal calcolaPrezzoSessioneSingola(SessioneSingolaAttivita sessione, Integer numeroPartecipanti) {
        BigDecimal prezzoBase = sessione.getSingolaAttivita().getPrezzo();
        BigDecimal partecipanti = BigDecimal.valueOf(numeroPartecipanti); // converto Big in un inter int se non erro
        // cosi posso moltiplicare (dannato Bigdecimal)
        return prezzoBase.multiply(partecipanti);
    }




    @Transactional
    public Prenotazione createPrenotazione(CrePrenotazioneRequest req) {



        return null;
    }
}
