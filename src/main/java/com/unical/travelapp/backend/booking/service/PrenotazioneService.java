package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.exception.*;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.booking.entity.*;
import com.unical.travelapp.backend.booking.repositories.ExtraPrenotazioneRepository;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.booking.dto.PartenzaOrganizzatoreDto;
import com.unical.travelapp.backend.catalog.entity.Attivita;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.exception.ItinerarioNonTrovatoException;
import com.unical.travelapp.backend.catalog.repository.AttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.DisponibilitaItinerarioRepository;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@AllArgsConstructor
@Service
public class PrenotazioneService {
    private final PrenotazioneRepository prenotazioneRepo;
    private final ExtraPrenotazioneRepository extraPrenotazioneRepo;
    private final AttivitaRepository attivitaRepo;
    private final UtenteRepository utenteRepository;
    private final DisponibilitaItinerarioRepository disponibilitaItinerarioRepository;
    private final ItinerarioRepository itinerarioRepository;
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

    // Oltre il termine fissato dall'organizzatore - o, se non ne ha fissato uno, oltre la
    // partenza - la disponibilita' non e' piu' prenotabile.
    private void controllaTerminePrenotazioni(DisponibilitaItinerario disp) {
        LocalDateTime termine = disp.getDataLimitePrenotazione() != null
                ? disp.getDataLimitePrenotazione()
                : disp.getDataInizio();

        if (termine != null && LocalDateTime.now().isAfter(termine)) {
            throw new RichiestaPrenotazioneNonValidaException(
                    "Le prenotazioni per questa partenza sono chiuse"
            );
        }
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

            controllaTerminePrenotazioni(disponibilitaItinerario);

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

    private boolean viaggioConcluso(Prenotazione prenotazione) {
        LocalDateTime dataFine = null;

        if (prenotazione.getDisponibilitaItinerario() != null) {
            dataFine = prenotazione
                    .getDisponibilitaItinerario()
                    .getDataFine();
        } else if (prenotazione.getSessioneSingolaAttivita() != null) {
            dataFine = prenotazione
                    .getSessioneSingolaAttivita()
                    .getDataFine();
        }

        return dataFine != null &&
                dataFine.isBefore(LocalDateTime.now());
    }

    @Transactional
    public Prenotazione annullaPrenotazione(Long prenotazioneId) {
        Prenotazione prenotazione = getPrenotazioneById(prenotazioneId);

        if (prenotazione.getStato().equals(StatoPrenotazione.CANCELLATA)) {
            throw new StatoPrenotazioneNonValidoException(
                    "Prenotazione già cancellata: " + prenotazioneId
            );
        }

        if (viaggioConcluso(prenotazione)) {
            throw new StatoPrenotazioneNonValidoException(
                    "Non puoi annullare una prenotazione relativa a un viaggio già concluso"
            );
        }

        if (prenotazione.getDisponibilitaItinerario() != null) {
            DisponibilitaItinerario disp =
                    prenotazione.getDisponibilitaItinerario();

            disp.setPostiDisponibili(
                    disp.getPostiDisponibili()
                            + prenotazione.getNumeroPartecipanti()
            );
        }
        if (prenotazione.getSessioneSingolaAttivita() != null) {
            SessioneSingolaAttivita sessione =
                    prenotazione.getSessioneSingolaAttivita();

            sessione.setPostiDisponibili(
                    sessione.getPostiDisponibili()
                            + prenotazione.getNumeroPartecipanti()
            );
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

    /**
     * I viaggi dell'utente gia' conclusi: data di fine passata e prenotazione non cancellata.
     *
     * <p>E' la lista da cui si lascia una recensione, quindi le cancellate restano fuori:
     * quel viaggio non e' mai stato fatto.
     */
    public Page<Prenotazione> getMieConcluse(Pageable pageable) {
        Long utenteId = utenteService.getUtenteSessione().getId();
        return prenotazioneRepo.findConcluseByViaggiatore(
                utenteId, LocalDateTime.now(), StatoPrenotazione.CANCELLATA, pageable);
    }

    /** Tutto il resto: viaggi in corso, futuri e prenotazioni cancellate. */
    public Page<Prenotazione> getMieAttuali(Pageable pageable) {
        Long utenteId = utenteService.getUtenteSessione().getId();
        return prenotazioneRepo.findAttualiByViaggiatore(
                utenteId, LocalDateTime.now(), StatoPrenotazione.CANCELLATA, pageable);
    }

    /*
     * ============================================================
     * VISTA ORGANIZZATORE
     * ============================================================
     *
     * L'organizzatore vede le partenze del proprio itinerario e, per ognuna, chi si e'
     * prenotato. Un ADMIN puo' guardare qualsiasi itinerario; chiunque altro solo i propri.
     */

    /**
     * Le partenze ancora da fare di un itinerario, dalla piu' vicina, con quante
     * prenotazioni ha raccolto ognuna.
     *
     * <p>Le partenze gia' concluse restano fuori: qui si guarda chi sta per partire, non
     * lo storico. Una partenza in corso e' ancora "da fare", quindi il confronto e' sulla
     * data di fine (o su quella di inizio, se la fine non e' valorizzata).
     */
    public List<PartenzaOrganizzatoreDto> getPartenzeFuture(Long itinerarioId) {
        verificaAccessoItinerario(itinerarioId);

        LocalDateTime adesso = LocalDateTime.now();

        List<DisponibilitaItinerario> future = disponibilitaItinerarioRepository
                .findByItinerario_Id(itinerarioId)
                .stream()
                .filter(disp -> !partenzaConclusa(disp, adesso))
                .sorted(Comparator.comparing(
                        DisponibilitaItinerario::getDataInizio,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (future.isEmpty()) {
            return List.of();
        }

        Map<Long, long[]> conteggi = conteggiPerDisponibilita(
                future.stream().map(DisponibilitaItinerario::getId).toList());

        return future.stream()
                .map(disp -> {
                    long[] conteggio = conteggi.getOrDefault(disp.getId(), new long[]{0L, 0L});
                    return PartenzaOrganizzatoreDto.builder()
                            .disponibilitaId(disp.getId())
                            .dataInizio(disp.getDataInizio())
                            .dataFine(disp.getDataFine())
                            .postiDisponibili(disp.getPostiDisponibili())
                            .numeroPrenotazioni(conteggio[0])
                            .partecipantiTotali(conteggio[1])
                            .build();
                })
                .toList();
    }

    /** Chi si e' prenotato su una partenza. Le cancellate non compaiono: nessuno parte. */
    public Page<Prenotazione> getPrenotazioniPerPartenza(Long disponibilitaId, Pageable pageable) {
        DisponibilitaItinerario disponibilita = recuperaDisponibilita(disponibilitaId);
        verificaAccessoItinerario(
                disponibilita.getItinerario() != null ? disponibilita.getItinerario().getId() : null);

        return prenotazioneRepo.findByDisponibilitaItinerario(
                disponibilitaId, StatoPrenotazione.CANCELLATA, pageable);
    }

    /**
     * Elimina una partenza dell'itinerario.
     *
     * <p>Si rifiuta se qualcuno l'ha prenotata: annullare il viaggio a chi l'ha comprato non
     * e' una cancellazione di calendario, e va fatto prima sulle singole prenotazioni. Anche
     * una prenotazione gia' cancellata blocca l'operazione, perche' resta nello storico del
     * viaggiatore e continua a riferirsi a questa partenza.
     */
    @Transactional
    public void eliminaPartenza(Long disponibilitaId) {
        DisponibilitaItinerario disponibilita = recuperaDisponibilita(disponibilitaId);
        Itinerario itinerario = disponibilita.getItinerario();

        verificaAccessoItinerario(itinerario != null ? itinerario.getId() : null);

        if (prenotazioneRepo.existsByDisponibilitaItinerario_Id(disponibilitaId)) {
            throw new PartenzaConPrenotazioniException(
                    "Non puoi eliminare una partenza che ha gia' delle prenotazioni");
        }

        // Tolta anche dalla collezione del padre: e' in cascade, e lasciarcela dentro la
        // farebbe risalvare al flush subito dopo averla cancellata.
        if (itinerario != null && itinerario.getDisponibilita() != null) {
            itinerario.getDisponibilita().remove(disponibilita);
        }

        disponibilitaItinerarioRepository.delete(disponibilita);
    }

    private boolean partenzaConclusa(DisponibilitaItinerario disp, LocalDateTime adesso) {
        LocalDateTime riferimento = disp.getDataFine() != null
                ? disp.getDataFine()
                : disp.getDataInizio();

        return riferimento != null && riferimento.isBefore(adesso);
    }

    private Map<Long, long[]> conteggiPerDisponibilita(List<Long> disponibilitaIds) {
        Map<Long, long[]> conteggi = new HashMap<>();

        for (Object[] riga : prenotazioneRepo.contaPerDisponibilita(
                disponibilitaIds, StatoPrenotazione.CANCELLATA)) {

            conteggi.put(
                    (Long) riga[0],
                    new long[]{((Number) riga[1]).longValue(), ((Number) riga[2]).longValue()});
        }

        return conteggi;
    }

    private void verificaAccessoItinerario(Long itinerarioId) {
        if (itinerarioId == null) {
            throw new ItinerarioNonTrovatoException("Itinerario non trovato");
        }

        if (utenteService.isAdmin()) {
            if (!itinerarioRepository.existsById(itinerarioId)) {
                throw new ItinerarioNonTrovatoException("Itinerario non trovato: " + itinerarioId);
            }
            return;
        }

        Long richiedenteId = utenteService.getUtenteSessione().getId();

        // Un itinerario di un altro organizzatore e' un 404 e non un 403: chi non lo ha
        // creato non deve nemmeno sapere che esiste.
        itinerarioRepository.findByIdAndOrganizzatore_Id(itinerarioId, richiedenteId)
                .orElseThrow(() -> new ItinerarioNonTrovatoException(
                        "Itinerario non trovato: " + itinerarioId));
    }

    public BigDecimal getSaldoTotaleGlobale() {
        return prenotazioneRepo.sumTotaleGlobale(StatoPrenotazione.CANCELLATA);
    }

    public BigDecimal getSaldoOrganizzatore() {
        Long organizzatoreId = utenteService.getUtenteSessione().getId();
        return prenotazioneRepo.sumTotalePerOrganizzatore(organizzatoreId, StatoPrenotazione.CANCELLATA);
    }
}