package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.repository.RecensioneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Job giornaliero che invita a recensire i viaggi conclusi il giorno prima.
 *
 * <p>E' idempotente per costruzione: prima di creare l'invito controlla che per quella
 * prenotazione non ne esista gia' uno e che l'utente non abbia gia' scritto la recensione.
 * Se il job gira due volte - stessa giornata, riavvio, esecuzione manuale - la seconda
 * volta non crea nulla.
 *
 * <p>La finestra e' l'intera giornata di ieri, mezzanotte inclusa ed esclusa il giorno
 * dopo: un viaggio che finisce alle 23:30 e uno che finisce alle 00:05 dello stesso giorno
 * ricevono l'invito insieme.
 */
@Component
public class InvitoRecensioneJob {

    private static final Logger log = LoggerFactory.getLogger(InvitoRecensioneJob.class);

    private final PrenotazioneRepository prenotazioneRepository;
    private final RecensioneRepository recensioneRepository;
    private final NotificaService notificaService;

    public InvitoRecensioneJob(PrenotazioneRepository prenotazioneRepository,
                               RecensioneRepository recensioneRepository,
                               NotificaService notificaService) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.recensioneRepository = recensioneRepository;
        this.notificaService = notificaService;
    }

    /**
     * Esecuzione programmata: una volta al giorno, sui viaggi finiti ieri.
     *
     * <p>Cadenza e fuso orario sono configurabili; il default e' le 9 del mattino, quando
     * la notifica ha piu' probabilita' di essere vista di una creata nel cuore della notte.
     */
    @Scheduled(cron = "${app.notifiche.invito-recensione.cron:0 0 9 * * *}",
            zone = "${app.notifiche.invito-recensione.zona:Europe/Rome}")
    @Transactional
    public void invitaARecensireIViaggiConclusiIeri() {
        int creati = generaInvitiPerViaggiConclusiIl(LocalDate.now().minusDays(1));
        log.info("Inviti a recensire generati: {}", creati);
    }

    /**
     * Genera gli inviti per i viaggi conclusi nel giorno indicato.
     *
     * @return quante notifiche sono state create davvero (le gia' presenti non contano)
     */
    @Transactional
    public int generaInvitiPerViaggiConclusiIl(LocalDate giorno) {
        LocalDateTime da = giorno.atStartOfDay();
        LocalDateTime a = giorno.plusDays(1).atStartOfDay();

        List<Prenotazione> concluse = prenotazioneRepository.findItinerariConclusiTra(
                da, a, StatoPrenotazione.CANCELLATA);

        if (concluse.isEmpty()) {
            return 0;
        }

        // Chi ha gia' recensito non va invitato: una sola query per tutte le prenotazioni
        // della giornata, invece di una per riga.
        Set<Long> giaRecensite = new HashSet<>(recensioneRepository
                .findByPrenotazione_IdIn(concluse.stream().map(Prenotazione::getId).toList())
                .stream()
                .map(recensione -> recensione.getPrenotazione().getId())
                .toList());

        int creati = 0;
        for (Prenotazione prenotazione : concluse) {
            if (giaRecensite.contains(prenotazione.getId())) {
                continue;
            }

            Itinerario itinerario = prenotazione.getDisponibilitaItinerario().getItinerario();
            if (notificaService.creaInvitoRecensione(prenotazione, itinerario)) {
                creati++;
            }
        }

        return creati;
    }
}
