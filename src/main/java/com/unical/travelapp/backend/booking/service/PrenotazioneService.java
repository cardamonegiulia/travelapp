package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
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

    private void validaRichiesta(CreaPrenotazioneRequest req){
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
    // al momento non sto usando il return quindi ho lascia cosi perche in futuro puo essre utile
    // o lo levo e faccio un metodo void
    private void creaPagamento (Prenotazione prenotazione, BigDecimal prezzoTotale) {
        Pagamento pay = Pagamento.builder()
                .prenotazione(prenotazione)
                .importo(prezzoTotale)
                .dataPagamento(LocalDateTime.now())
                .stato(StatoPagamento.IN_ATTESA)
                .build();
        pagamentoRepo.save(pay);
    }

    @Transactional
    public Prenotazione createPrenotazione(CreaPrenotazioneRequest req) {
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

    public Prenotazione getPrenotazioneById(Long id){
        Optional<Prenotazione> prenotazioneId = prenotazioneRepo.findById(id);
        if(prenotazioneId.isPresent()) {
            return prenotazioneId.get();
        }
        throw new IllegalArgumentException("Prenotazione non trovata: " + id);
    }

    public List<Prenotazione> getPrenotazioniByUtente(Long utenteId) {
        recuperaUtente(utenteId);
        return prenotazioneRepo.findByViaggiatoreId(utenteId);
    }

    // la firma è Prenotazione perchè dopo il pagamento il frontend vuole vedere la prenotazione aggiornata
    // ma si poteva anche usare Pagamento ma non credo sia coerente per il motivo sopra riportato
    @Transactional
    public Prenotazione pagaPrenotazione(Long prenotazioneId){
        Optional<Pagamento> pagamento = pagamentoRepo.findByPrenotazioneId(prenotazioneId);

        if(!pagamento.isPresent()){
            throw new IllegalArgumentException("Pagamento non trovato: " + prenotazioneId);
        }

        Pagamento pay = pagamento.get();
        Prenotazione prenotazione = pay.getPrenotazione();

        if(prenotazione.getStato().equals(StatoPrenotazione.CANCELLATA)){
            throw new IllegalArgumentException("Non puoi pagare una prenotazione cancellata: " + prenotazioneId);
        }

        if(pay.getStato().equals(StatoPagamento.COMPLETATO)){
            throw new IllegalArgumentException("Pagamento gia completato: " + prenotazioneId);
        }

        pay.setStato(StatoPagamento.COMPLETATO);
        pay.setDataPagamento(LocalDateTime.now());

        prenotazione.setStato(StatoPrenotazione.CONFERMATA);

        pagamentoRepo.save(pay);
        return prenotazioneRepo.save(prenotazione);
    }

    // qui gestisco l'annullamento di una prenotazione, da capire se posso compattarla
    @Transactional
    public Prenotazione annullaPrenotazione(Long prenotazioneId) {
        Prenotazione prenotazione = getPrenotazioneById(prenotazioneId);

        if(prenotazione.getStato().equals(StatoPrenotazione.CANCELLATA)) {
            throw new IllegalArgumentException("Prenotazione gia cancellata: " + prenotazioneId);
        }

        //se itinerario ripristino i posti, stessa cosa dopo con sessione singola attività
        if(prenotazione.getDisponibilitaItinerario() != null) {
            DisponibilitaItinerario disp = prenotazione.getDisponibilitaItinerario();
            disp.setPostiDisponibili(disp.getPostiDisponibili() + prenotazione.getNumeroPartecipanti());
        }

        if(prenotazione.getSessioneSingolaAttivita() != null) {
            SessioneSingolaAttivita sessione = prenotazione.getSessioneSingolaAttivita();
            sessione.setPostiDisponibili(sessione.getPostiDisponibili() + prenotazione.getNumeroPartecipanti());
        }

        // qui recupero il pagamento e poi controllo se esiste e in base allo stato effettuo un rimborso o altro
        Optional<Pagamento> pagamento = pagamentoRepo.findByPrenotazioneId(prenotazioneId);

        if(pagamento.isPresent()) {
            Pagamento pay = pagamento.get();

            if(pay.getStato().equals(StatoPagamento.COMPLETATO)) {
                pay.setStato(StatoPagamento.RIMBORSATO);
            } else if(pay.getStato().equals(StatoPagamento.IN_ATTESA)) {
                pay.setStato(StatoPagamento.FALLITO);
            }

            pagamentoRepo.save(pay);
        }

        prenotazione.setStato(StatoPrenotazione.CANCELLATA);

        return prenotazioneRepo.save(prenotazione);
    }


}
