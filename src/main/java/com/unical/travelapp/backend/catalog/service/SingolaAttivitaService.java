package com.unical.travelapp.backend.catalog.service;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.catalog.entity.SessioneSingolaAttivita;
import com.unical.travelapp.backend.catalog.exception.SingolaAttivitaNonTrovataException;
import com.unical.travelapp.backend.catalog.repository.SingolaAttivitaRepository;
import com.unical.travelapp.backend.catalog.repository.SessioneSingolaAttivitaRepository;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import com.unical.travelapp.backend.experience.mapper.ImmagineMapper;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import com.unical.travelapp.backend.experience.models.Immagine;
import com.unical.travelapp.backend.experience.services.ImmagineService;
import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
@Service
public class SingolaAttivitaService {
    private static final long MASSIMO_GIORNI_PROGRAMMABILI = 366;
    @Autowired
    private SingolaAttivitaRepository singolaAttivitaRepository;
    @Autowired
    private SessioneSingolaAttivitaRepository sessioneRepository;
    @Autowired
    private ImmagineService immagineService;
    @Autowired
    private ImmagineMapper immagineMapper;
    public Page<SingolaAttivita> getAllAttivita(Pageable pageable) {
        return singolaAttivitaRepository.findAll(pageable);
    }
    public Optional<SingolaAttivita> getAttivitaById(Long id) {
        return singolaAttivitaRepository.findById(id);
    }
    public List<SessioneSingolaAttivita> getSessioniByAttivitaId(Long attivitaId) {
        if (!singolaAttivitaRepository.existsById(attivitaId)) {
            throw new SingolaAttivitaNonTrovataException("Attività non trovata: " + attivitaId);
        }
        return sessioneRepository.findBySingolaAttivita_Id(attivitaId);
    }
    @Transactional
    public SingolaAttivita saveAttivitaConSessioni(SingolaAttivita attivita, LocalDate dataInizio, LocalDate dataFine, List<Integer> giorniSettimana) {
        if (dataFine.isBefore(dataInizio)) {
            throw new IllegalArgumentException("La data di fine non può essere antecedente alla data di inizio");
        }
        if (ChronoUnit.DAYS.between(dataInizio, dataFine) > MASSIMO_GIORNI_PROGRAMMABILI) {
            throw new IllegalArgumentException("L'intervallo non può superare " + MASSIMO_GIORNI_PROGRAMMABILI + " giorni");
        }
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
    public SingolaAttivita updateAttivita(Long id, SingolaAttivita datiAggiornati, Utente richiedente, boolean isAdmin) {
        SingolaAttivita esistente = attivitaModificabile(id, richiedente, isAdmin);
        esistente.setTitolo(datiAggiornati.getTitolo());
        esistente.setDescrizione(datiAggiornati.getDescrizione());
        esistente.setLuogo(datiAggiornati.getLuogo());
        esistente.setPrezzo(datiAggiornati.getPrezzo());
        esistente.setDurataMinuti(datiAggiornati.getDurataMinuti());
        esistente.setMaxPartecipanti(datiAggiornati.getMaxPartecipanti());
        return singolaAttivitaRepository.save(esistente);
    }
    @Transactional
    public void deleteAttivita(Long id, Utente richiedente, boolean isAdmin) {
        SingolaAttivita attivita = attivitaModificabile(id, richiedente, isAdmin);
        if (attivita.getImmagini() != null) {
            immagineService.eliminaTutte(attivita.getImmagini());
            attivita.getImmagini().clear();
        }
        singolaAttivitaRepository.delete(attivita);
    }
    @Transactional(timeoutString = "${app.storage.immagini.upload-timeout-secondi:30}")
    public ImmagineResponse aggiungiImmagine(Long id, MultipartFile file, Utente richiedente, boolean isAdmin) {
        SingolaAttivita attivita = attivitaModificabile(id, richiedente, isAdmin);
        immagineService.verificaLimite(attivita.getImmagini() != null ? attivita.getImmagini().size() : 0);
        Immagine immagine = immagineService.caricaEntita(file);
        attivita.getImmagini().add(immagine);
        singolaAttivitaRepository.saveAndFlush(attivita);
        return immagineMapper.toResponse(immagine);
    }
    public List<ImmagineResponse> getImmagini(Long id) {
        SingolaAttivita attivita = singolaAttivitaRepository.findById(id)
                .orElseThrow(() -> new SingolaAttivitaNonTrovataException("Attività non trovata: " + id));
        return immagineMapper.toResponse(attivita.getImmagini());
    }
    @Transactional
    public void rimuoviImmagine(Long id, Long immagineId, Utente richiedente, boolean isAdmin) {
        SingolaAttivita attivita = attivitaModificabile(id, richiedente, isAdmin);
        Immagine immagine = attivita.getImmagini().stream()
                .filter(i -> i.getId().equals(immagineId))
                .findFirst()
                .orElseThrow(() -> new ImmagineNonTrovata("Immagine non trovata sull'attività: " + immagineId));
        attivita.getImmagini().remove(immagine);
        singolaAttivitaRepository.saveAndFlush(attivita);
        immagineService.eliminaEntita(immagine);
    }
    private SingolaAttivita attivitaModificabile(Long id, Utente richiedente, boolean isAdmin) {
        if (isAdmin) {
            return singolaAttivitaRepository.findById(id)
                    .orElseThrow(() -> new SingolaAttivitaNonTrovataException("Attività non trovata: " + id));
        }
        return singolaAttivitaRepository.findByIdAndOrganizzatore_Id(id, richiedente.getId())
                .orElseThrow(() -> new SingolaAttivitaNonTrovataException("Attività non trovata: " + id));
    }
}
