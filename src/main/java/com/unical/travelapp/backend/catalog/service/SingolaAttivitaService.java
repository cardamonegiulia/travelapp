package com.unical.travelapp.backend.catalog.service;

import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.repository.SingolaAttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SingolaAttivitaService {

    @Autowired
    private SingolaAttivitaRepository singolaAttivitaRepository;

    @Autowired
    private SessioneSingolaAttivitaRepository sessioneRepository;

    public List<SingolaAttivita> getAllAttivita() {
        return singolaAttivitaRepository.findAll();
    }

    public Optional<SingolaAttivita> getAttivitaById(Long id) {
        return singolaAttivitaRepository.findById(id);
    }

    @Transactional
    public SingolaAttivita saveAttivitaConSessioni(SingolaAttivita attivita, LocalDate dataInizio, LocalDate dataFine, List<Integer> giorniSettimana) {

        if (dataFine.isBefore(dataInizio)) {
            throw new IllegalArgumentException("La data di fine non può essere antecedente alla data di inizio");
        } //se l'organizzatore sbaglia e mette una data fine che viene prima di una data inizio lancia questa eccezione

        SingolaAttivita attivitaSalvata = singolaAttivitaRepository.save(attivita);

        LocalDate dataCorrente = dataInizio;

        while (!dataCorrente.isAfter(dataFine)) {
            int giornoAttualeValore = dataCorrente.getDayOfWeek().getValue();

            if (giorniSettimana.contains(giornoAttualeValore)) {
                SessioneSingolaAttivita nuovaSessione = new SessioneSingolaAttivita();
                nuovaSessione.setDataInizio(dataCorrente.atStartOfDay());
                nuovaSessione.setDataFine(dataCorrente.atTime(23, 59, 59));
                nuovaSessione.setPostiDisponibili(attivitaSalvata.getMaxPartecipanti());
                nuovaSessione.setSingolaAttivita(attivitaSalvata);
                nuovaSessione.setStato("ATTIVA");

                sessioneRepository.save(nuovaSessione);
            }

            dataCorrente = dataCorrente.plusDays(1);
        }

        return attivitaSalvata;
    }

    @Transactional
    public void deleteAttivita(Long id) {
        singolaAttivitaRepository.deleteById(id);
    }
}