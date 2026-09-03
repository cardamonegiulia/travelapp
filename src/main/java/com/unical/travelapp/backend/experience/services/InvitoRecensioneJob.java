package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.repository.RecensioneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
public class InvitoRecensioneJob {

    private static final Logger log =
            LoggerFactory.getLogger(InvitoRecensioneJob.class);

    private final PrenotazioneRepository prenotazioneRepository;
    private final RecensioneRepository recensioneRepository;
    private final NotificaService notificaService;
    private final ZoneId zonaNotifiche;

    public InvitoRecensioneJob(
            PrenotazioneRepository prenotazioneRepository,
            RecensioneRepository recensioneRepository,
            NotificaService notificaService,
            @Value("${app.notifiche.invito-recensione.zona:Europe/Rome}")
            String zonaNotifiche
    ) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.recensioneRepository = recensioneRepository;
        this.notificaService = notificaService;
        this.zonaNotifiche = ZoneId.of(zonaNotifiche);
    }


    @Scheduled(
            cron = "${app.notifiche.invito-recensione.cron:0 0 9 * * *}",
            zone = "${app.notifiche.invito-recensione.zona:Europe/Rome}"
    )
    @Transactional
    public void generaInvitiRecensioneProgrammato() {

        int creati = generaInvitiMancantiFinoAIeri();

        log.info(
                "Inviti a recensire generati dal job programmato: {}",
                creati
        );
    }


    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recuperaInvitiMancantiAllAvvio() {

        int creati = generaInvitiMancantiFinoAIeri();

        log.info(
                "Recupero notifiche all'avvio completato. Inviti generati: {}",
                creati
        );
    }


    @Transactional
    public int generaInvitiMancantiFinoAIeri() {

        LocalDate oggi = LocalDate.now(zonaNotifiche);

        LocalDateTime inizioOggi =
                oggi.atStartOfDay();

        List<Prenotazione> concluse =
                prenotazioneRepository.findItinerariConclusiPrimaDi(
                        inizioOggi,
                        StatoPrenotazione.CANCELLATA
                );

        return generaInviti(concluse);
    }

    @Transactional
    public int generaInvitiPerViaggiConclusiIl(LocalDate giorno) {

        LocalDateTime da =
                giorno.atStartOfDay();

        LocalDateTime a =
                giorno.plusDays(1).atStartOfDay();

        List<Prenotazione> concluse =
                prenotazioneRepository.findItinerariConclusiTra(
                        da,
                        a,
                        StatoPrenotazione.CANCELLATA
                );

        return generaInviti(concluse);
    }

    private int generaInviti(List<Prenotazione> concluse) {

        if (concluse == null || concluse.isEmpty()) {
            return 0;
        }

        List<Long> prenotazioneIds =
                concluse.stream()
                        .map(Prenotazione::getId)
                        .toList();

        Set<Long> giaRecensite =
                new HashSet<>(
                        recensioneRepository
                                .findByPrenotazione_IdIn(prenotazioneIds)
                                .stream()
                                .map(recensione ->
                                        recensione
                                                .getPrenotazione()
                                                .getId()
                                )
                                .toList()
                );

        int creati = 0;

        for (Prenotazione prenotazione : concluse) {

            if (giaRecensite.contains(prenotazione.getId())) {
                continue;
            }

            if (prenotazione.getDisponibilitaItinerario() == null) {
                continue;
            }

            Itinerario itinerario =
                    prenotazione
                            .getDisponibilitaItinerario()
                            .getItinerario();

            if (itinerario == null) {
                continue;
            }

            if (notificaService.creaInvitoRecensione(
                    prenotazione,
                    itinerario
            )) {
                creati++;
            }
        }

        return creati;
    }
}