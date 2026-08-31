package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.exception.*;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.entity.*;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.Attivita;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    private final PrenotazioneRepository prenotazioneRepo;
    private final ExtraPrenotazioneRepository extraPrenotazioneRepo;
    private final AttivitaRepository attivitaRepo;
    private final UtenteRepository utenteRepository;
    private final DisponibilitaItinerarioRepository disponibilitaItinerarioRepository;
    private final SessioneSingolaAttivitaRepository sessioneSingolaAttivitaRepository;
    private final UtenteService utenteService;
    private final PagamentoService pagamentoService;

    private void verificaEsistenzaUtente(Long id) {
        if (!utenteRepository.existsById(id)) {
            throw new UtenteNonTrovatoException(
                    "Utente non trovato: " + id
            );
        }
    }

    private void validaRichiesta(CreaPrenotazioneRequest req){
        if(req.getNumeroPartecipanti() == null || req.getNumeroPartecipanti() <= 0){
            throw new RichiestaPrenotazioneNonValidaException("Numero partecipanti non valido");
        }

        if(req.getDisponibilitaItinerarioId() == null && req.getSessioneSingolaAttivitaId() == null ){
            throw new RichiestaPrenotazioneNonValidaException("Devi selezionare un itinerario o una singola attività");
        }

        if(req.getDisponibilitaItinerarioId() != null && req.getSessioneSingolaAttivitaId() != null ){
            throw new RichiestaPrenotazioneNonValidaException("Devi selezionare un itinerario o una singola attività");
        }

        if(req.getSessioneSingolaAttivitaId() != null && req.getAttivitaExtraIds() != null && !req.getAttivitaExtraIds().isEmpty()){
            throw new RichiestaPrenotazioneNonValidaException("Non ci sono attività extra per le singole attività");
        }

        if (req.getAttivitaExtraIds() != null) {
            long distinti = req.getAttivitaExtraIds()
                    .stream()
                    .distinct()
                    .count();

            if (distinti != req.getAttivitaExtraIds().size()) {
                throw new RichiestaPrenotazioneNonValidaException(
                        "Non puoi inserire due volte la stessa attività extra"
                );
            }
        }
    }

    private DisponibilitaItinerario recuperaDisponibilita(Long id){
        return disponibilitaItinerarioRepository.findById(id).orElseThrow(()-> new DisponibilitaNonTrovataException("Disponibilità itinerario non trovata: " +id));
    }

    private SessioneSingolaAttivita recuperaSingolaAttivita(Long id){
        return sessioneSingolaAttivitaRepository.findById(id).orElseThrow(()-> new DisponibilitaNonTrovataException("Sessione singola non trovato: "+ id));
    }

    private int calcolaPostiResidui(
            Integer postiDisponibili,
            Integer numeroPartecipanti) {

        if (postiDisponibili < numeroPartecipanti) {
            throw new PostiInsufficientiException(
                    "Numero posti non disponibili"
            );
        }

        return postiDisponibili - numeroPartecipanti;
    }


    private void controllaEScalaPostiSessione(SessioneSingolaAttivita sessione, Integer numeroPartecipanti){
        sessione.setPostiDisponibili(calcolaPostiResidui(sessione.getPostiDisponibili(),numeroPartecipanti));
    }

    private void controllaEScalaPostiItinerario(DisponibilitaItinerario disp, Integer numeroPartecipanti) {
        disp.setPostiDisponibili(calcolaPostiResidui(disp.getPostiDisponibili(), numeroPartecipanti));
    }

    private BigDecimal calcolaPrezzoItinerario(DisponibilitaItinerario disp, Integer numeroPartecipanti) {
        BigDecimal prezzoBase = disp.getItinerario().getPrezzoBase();
        BigDecimal partecipanti = BigDecimal.valueOf(numeroPartecipanti);
        return prezzoBase.multiply(partecipanti);
    }

    private BigDecimal calcolaPrezzoSessioneSingola(SessioneSingolaAttivita sessione, Integer numeroPartecipanti) {
        BigDecimal prezzoBase = sessione.getSingolaAttivita().getPrezzo();
        BigDecimal partecipanti = BigDecimal.valueOf(numeroPartecipanti);
        return prezzoBase.multiply(partecipanti);
    }

    private Attivita recuperaEValidaAttivitaExtra(Long id, DisponibilitaItinerario disp) {
        Optional<Attivita> optionalAtt = attivitaRepo.findById(id);

        if(optionalAtt.isEmpty()) {
            throw new AttivitaExtraNonValidaException("Attività extra non trovata: " + id);
        }

        Attivita att = optionalAtt.get();

        if(att.getTappa() == null || att.getTappa().getItinerario() == null){
            throw new AttivitaExtraNonValidaException("Tappa o itinerario non associati all'attività: " + id);
        }

        if(!att.getTappa().getItinerario().getId().equals(disp.getItinerario().getId())){
            throw new AttivitaExtraNonValidaException("Attività non inerente all'itinerario scelto");
        }

        if (att.isObbligatoria()) {
            throw new AttivitaExtraNonValidaException(
                    "Un'attività obbligatoria non può essere selezionata come extra"
            );
        }

        if (att.getPrezzoExtra() == null) {
            throw new AttivitaExtraNonValidaException(
                    "L'attività selezionata non ha un prezzo extra valido"
            );
        }

        return att;
    }

    private List<Attivita> recuperaEValidaAttivitaExtra(
            List<Long> extraIds,
            DisponibilitaItinerario disponibilita) {

        if (extraIds == null || extraIds.isEmpty()) {
            return List.of();
        }

        return extraIds.stream()
                .map(id -> recuperaEValidaAttivitaExtra(id, disponibilita))
                .toList();
    }


    private BigDecimal calcolaPrezzoExtra(List<Attivita> attivitaExtra, Integer numeroPartecipanti) {
        BigDecimal totale = BigDecimal.ZERO;
        for (Attivita attivita : attivitaExtra) {
            totale = totale.add(attivita.getPrezzoExtra().multiply(BigDecimal.valueOf(numeroPartecipanti)));
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

    private void creaExtraPrenotazione(
            Prenotazione prenotazione,
            List<Attivita> attivitaExtra) {

        for (Attivita attivita : attivitaExtra) {

            ExtraPrenotazione extra = ExtraPrenotazione.builder()
                    .prenotazione(prenotazione)
                    .attivita(attivita)
                    .prezzoExtra(attivita.getPrezzoExtra())
                    .build();

            extraPrenotazioneRepo.save(extra);
        }
    }

    @Transactional
    public Prenotazione createPrenotazione(CreaPrenotazioneRequest req) {
        validaRichiesta(req);

        Utente viaggiatore = utenteService.getUtenteSessione();

        boolean isItinerario = req.getDisponibilitaItinerarioId() != null;

        DisponibilitaItinerario disponibilitaItinerario = null;
        SessioneSingolaAttivita sessioneSingolaAttivita = null;

        BigDecimal prezzoBase;
        BigDecimal prezzoTotale;

        List<Attivita> attivitaExtra = List.of();

        if (isItinerario) {
            disponibilitaItinerario =
                    recuperaDisponibilita(req.getDisponibilitaItinerarioId());

            controllaEScalaPostiItinerario(
                    disponibilitaItinerario,
                    req.getNumeroPartecipanti()
            );

            prezzoBase = calcolaPrezzoItinerario(
                    disponibilitaItinerario,
                    req.getNumeroPartecipanti()
            );

            attivitaExtra = recuperaEValidaAttivitaExtra(
                    req.getAttivitaExtraIds(),
                    disponibilitaItinerario
            );

            BigDecimal prezzoExtra = calcolaPrezzoExtra(
                    attivitaExtra,
                    req.getNumeroPartecipanti()
            );

            prezzoTotale = prezzoBase.add(prezzoExtra);

        } else {
            sessioneSingolaAttivita =
                    recuperaSingolaAttivita(req.getSessioneSingolaAttivitaId());

            controllaEScalaPostiSessione(
                    sessioneSingolaAttivita,
                    req.getNumeroPartecipanti()
            );

            prezzoBase = calcolaPrezzoSessioneSingola(
                    sessioneSingolaAttivita,
                    req.getNumeroPartecipanti()
            );

            prezzoTotale = prezzoBase;
        }

        Prenotazione prenotazione = creaEntityPrenotazione(
                viaggiatore,
                disponibilitaItinerario,
                sessioneSingolaAttivita,
                prezzoTotale,
                req.getNumeroPartecipanti()
        );

        Prenotazione prenotazioneSave =
                prenotazioneRepo.save(prenotazione);

        if (isItinerario) {
            creaExtraPrenotazione(
                    prenotazioneSave,
                    attivitaExtra
            );
        }

        pagamentoService.creaPagamento(
                prenotazioneSave,
                prezzoTotale
        );

        return prenotazioneSave;
    }

    public Prenotazione getPrenotazioneById(Long id){
        if (utenteService.isAdmin()) {
            return prenotazioneRepo.findById(id)
                    .orElseThrow(() -> new PrenotazioneNonTrovataException("Prenotazione non trovata: " + id));
        }

        Long viaggiatoreId = utenteService.getUtenteSessione().getId();
        return prenotazioneRepo.findByIdAndViaggiatoreId(id, viaggiatoreId)
                .orElseThrow(() -> new PrenotazioneNonTrovataException("Prenotazione non trovata: " + id));
    }

    public Page<Prenotazione> getPrenotazioniByUtente(Long utenteId, Pageable pageable) {
        Utente richiedente = utenteService.getUtenteSessione();
        if (!utenteService.isAdmin() && !richiedente.getId().equals(utenteId)) {
            throw new AccessDeniedException("Non puoi consultare le prenotazioni di un altro utente");
        }
        verificaEsistenzaUtente(utenteId);
        return prenotazioneRepo.findByViaggiatoreId(utenteId, pageable);
    }

    @Transactional
    public Prenotazione annullaPrenotazione(Long prenotazioneId) {
        Prenotazione prenotazione = getPrenotazioneById(prenotazioneId);

        if(prenotazione.getStato().equals(StatoPrenotazione.CANCELLATA)) {
            throw new StatoPrenotazioneNonValidoException("Prenotazione già cancellata: " + prenotazioneId);
        }

        if(prenotazione.getDisponibilitaItinerario() != null) {
            DisponibilitaItinerario disp = prenotazione.getDisponibilitaItinerario();
            disp.setPostiDisponibili(disp.getPostiDisponibili() + prenotazione.getNumeroPartecipanti());
        }

        if(prenotazione.getSessioneSingolaAttivita() != null) {
            SessioneSingolaAttivita sessione = prenotazione.getSessioneSingolaAttivita();
            sessione.setPostiDisponibili(sessione.getPostiDisponibili() + prenotazione.getNumeroPartecipanti());
        }
        pagamentoService.gestisciPagamentoAnnullamento(prenotazioneId);
        prenotazione.setStato(StatoPrenotazione.CANCELLATA);
        return prenotazioneRepo.save(prenotazione);
    }

    public Page<Prenotazione> getMiePrenotazioni(Pageable pageable) {
        Long utenteId = utenteService
                .getUtenteSessione()
                .getId();

        return prenotazioneRepo.findByViaggiatoreId(
                utenteId,
                pageable
        );
    }

    public BigDecimal getSaldoTotaleGlobale() {
        return prenotazioneRepo.sumTotaleGlobale(StatoPrenotazione.CANCELLATA);
    }

    public BigDecimal getSaldoOrganizzatore() {
        Long organizzatoreId = utenteService.getUtenteSessione().getId();
        return prenotazioneRepo.sumTotalePerOrganizzatore(organizzatoreId, StatoPrenotazione.CANCELLATA);
    }
}