package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.entity.StatoPrenotazione;
import com.unical.travelapp.backend.booking.repositories.PrenotazioneRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class PrenotazioniScaduteJob {

    private final PrenotazioneRepository prenotazioneRepository;
    private final PrenotazioneService prenotazioneService;

    @Scheduled(
            fixedDelay = 60_000,
            initialDelay = 60_000
    )
    public void controllaPrenotazioniScadute() {
        pulisciPrenotazioniScadute();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void controllaPrenotazioniScaduteAllAvvio() {
        pulisciPrenotazioniScadute();
    }

    private void pulisciPrenotazioniScadute() {

        LocalDateTime limite =
                LocalDateTime.now()
                        .minusMinutes(
                                PrenotazioneService.MINUTI_SCADENZA_PAGAMENTO
                        );

        List<Long> ids =
                prenotazioneRepository.findIdsPrenotazioniScadute(
                        StatoPrenotazione.IN_ATTESA,
                        limite
                );

        int annullate = 0;

        for (Long id : ids) {
            try {
                prenotazioneService.annullaPrenotazioneScaduta(
                        id,
                        limite
                );

                annullate++;

            } catch (Exception e) {
                log.warn(
                        "Errore durante l'annullamento automatico della prenotazione {}",
                        id,
                        e
                );
            }
        }

        if (annullate > 0) {
            log.info(
                    "Prenotazioni scadute annullate automaticamente: {}",
                    annullate
            );
        }
    }
}