package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.dto.CrePrenotazioneRequest;
import com.unical.travelapp.backend.booking.entity.*;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.Attivita;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class PrenotazioneService {
    // qui mettero dentro i repository per parlare con il DB

    private final PrenotazioneRepository prenotazioneRepo;
    private final ExtraPrenotazioneRepository extraPrenotazioneRepo;
    private final PagamentoRepository pagamentoRepo;
    private final AttivitaRepository attivitaRepo;
    private final UtenteRepository utenteRepository;
    private final DisponibilitaItinerarioRepository disponibilitaItinerarioRepository;
    private final SessioneSingolaAttivitaRepository sessioneSingolaAttivitaRepository;


    // cretePrenotazione era diventata troppo grande quindi ora creo diversi piccoli metodi
    // cosi da avere una logica pulita e leggibile da usare poi dentro createPrenotazione.

    private Utente recuperaUtente(Long id) {
        Optional<Utente> utente = utenteRepository.findById(id);

        if (!utente.isPresent()) {
            throw new IllegalArgumentException("Utente non trovato");
        }

        return utente.get();
    }

    private void validaRichiesta(CrePrenotazioneRequest req){
        // qui controllo se numeroPartecipanti presente e > 0
        if(req.getNumeroPartecipanti() == null || req.getNumeroPartecipanti() <= 0){
            throw new IllegalArgumentException  ("numeri posti non disponibili");
        }
        // qui che almeno uno tra itinerario/sessione è selezionato
        if(req.getDisponibilitaItinerarioId() == null && req.getSessioneSingolaAttivitaId() == null ){
            throw new IllegalArgumentException ("Devi selezionare un itinerario o una singola attività");
        }
        // qui che non possono essere entrambi insieme/selezionati
        if(req.getDisponibilitaItinerarioId() != null && req.getSessioneSingolaAttivitaId() != null ){
            throw new IllegalArgumentException ("Devi selezionare un itinerario o una singola attività");
        }
        // qui vieto gli extra per sessione singola
        if(req.getSessioneSingolaAttivitaId() != null && req.getAttivitaExtraIds() != null && !req.getAttivitaExtraIds().isEmpty()){
            throw new IllegalArgumentException("Non ci sono attività extra per le singole attività");
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

    private Attivita recuperaEValidaAttivitaExtra(Long id, DisponibilitaItinerario disp) {
        Optional<Attivita> optionalAtt = attivitaRepo.findById(id);

        if(!optionalAtt.isPresent()) {
            throw new IllegalArgumentException("Attivita non trovata: " + id);
        }

        Attivita att = optionalAtt.get();

        if(att.getTappa() == null || att.getTappa().getItinerario() == null){
            throw new IllegalArgumentException("Tappa o itinerario non associati all'attività: " + id);
        }

        if(!att.getTappa().getItinerario().getId().equals(disp.getItinerario().getId())){
            throw new IllegalArgumentException("Attivita non inerente all'itinerario scelto!");
        }

        return att;
    }

    private BigDecimal calcolaPrezzoExtra(List<Long> extraIds, Integer numeroPartecipanti, DisponibilitaItinerario disp) {
        BigDecimal totale = BigDecimal.ZERO;

        if(extraIds == null || extraIds.isEmpty()) return BigDecimal.ZERO;

        for(Long id : extraIds) {
            Attivita att = recuperaEValidaAttivitaExtra(id, disp);
            totale = totale.add(att.getPrezzoExtra().multiply(BigDecimal.valueOf(numeroPartecipanti)));
        }

        return totale;
    }

    private Prenotazione creaEntityPrenotazione (Utente viaggiatore, DisponibilitaItinerario disp, SessioneSingolaAttivita sessione, BigDecimal prezzoTotale, Integer numeroPartecipanti) {
        return Prenotazione.builder()
                .viaggiatore(viaggiatore)
                .disponibilitaItinerario(disp)
                .sessioneSingolaAttivita(sessione)
                .numeroPartecipanti(numeroPartecipanti)
                .prezzoTotale(prezzoTotale)
                .stato(StatoPrenotazione.IN_ATTESA)
                .dataPrenotazione(LocalDateTime.now())
                .build();
    }

    private void creaExtraPrenotazione(Prenotazione prenotazione, List<Long> extraIds, DisponibilitaItinerario disp) {
        if(extraIds == null || extraIds.isEmpty()) return;

        for(Long id : extraIds) {
            Attivita att = recuperaEValidaAttivitaExtra(id, disp);

            ExtraPrenotazione extra = ExtraPrenotazione.builder()
                    .prenotazione(prenotazione)
                    .attivita(att)
                    .prezzoExtra(att.getPrezzoExtra())
                    .build();

            extraPrenotazioneRepo.save(extra);
        }
    }

    private Pagamento creaPagamento (Prenotazione prenotazione, BigDecimal prezzoTotale) {
        Pagamento pay = Pagamento.builder()
                .prenotazione(prenotazione)
                .importo(prezzoTotale)
                .dataPagamento(LocalDateTime.now())
                .stato(StatoPagamento.IN_ATTESA)
                .build();
        pagamentoRepo.save(pay);

        return pay;
    }


    @Transactional
    public Prenotazione createPrenotazione(CrePrenotazioneRequest req) {
        validaRichiesta(req);
        Utente viaggiatore = recuperaUtente(req.getViaggiatoreId());

        boolean isItinerario = req.getDisponibilitaItinerarioId() != null;

        DisponibilitaItinerario disponibilitaItinerario = null;
        SessioneSingolaAttivita sessioneSingolaAttivita = null;
        BigDecimal prezzoBase;
        BigDecimal prezzoExtra;
        BigDecimal prezzoTotale;

        if(isItinerario) {
            disponibilitaItinerario = recuperaDisponibilita(req.getDisponibilitaItinerarioId());
            controllaEScalaPostiItinerario(disponibilitaItinerario, req.getNumeroPartecipanti());
            prezzoBase = calcolaPrezzoItinerario(disponibilitaItinerario, req.getNumeroPartecipanti());
            prezzoExtra = calcolaPrezzoExtra(req.getAttivitaExtraIds(), req.getNumeroPartecipanti(), disponibilitaItinerario);
            prezzoTotale = prezzoBase.add(prezzoExtra);
        }else{
            sessioneSingolaAttivita = recuperaSingolaAttivita(req.getSessioneSingolaAttivitaId());
            controllaEScalaPostiSessione(sessioneSingolaAttivita, req.getNumeroPartecipanti());
            prezzoBase = calcolaPrezzoSessioneSingola(sessioneSingolaAttivita, req.getNumeroPartecipanti());
            prezzoTotale = prezzoBase;
        }

        Prenotazione prenotazione= creaEntityPrenotazione(viaggiatore,disponibilitaItinerario, sessioneSingolaAttivita, prezzoTotale, req.getNumeroPartecipanti());
        Prenotazione prenotazioneSave = prenotazioneRepo.save(prenotazione);

        if(isItinerario){
            creaExtraPrenotazione(prenotazioneSave, req.getAttivitaExtraIds(), disponibilitaItinerario);
        }
        creaPagamento(prenotazioneSave, prezzoTotale);
        return prenotazioneSave;
    }
}
