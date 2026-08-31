package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.experience.models.Notifica;
import com.unical.travelapp.backend.experience.models.TipoNotifica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificaRepository extends JpaRepository<Notifica, Long> {

    /** Le notifiche di un utente, dalla piu' recente. Sempre filtrate per destinatario: e' il controllo anti-BOLA. */
    Page<Notifica> findByDestinatario_IdOrderByIdDesc(Long destinatarioId, Pageable pageable);

    /** Una notifica, ma solo se e' davvero del chiamante. */
    Optional<Notifica> findByIdAndDestinatario_Id(Long id, Long destinatarioId);

    long countByDestinatario_IdAndLettaFalse(Long destinatarioId);

    /**
     * Serve al job: se l'invito per quella prenotazione esiste gia' non se ne crea un altro,
     * cosi' due esecuzioni nello stesso giorno (o un rerun manuale) non generano duplicati.
     */
    boolean existsByPrenotazione_IdAndTipo(Long prenotazioneId, TipoNotifica tipo);

    /** Prenotazioni, fra quelle passate, per cui l'invito e' gia' stato creato: una sola query invece di N. */
    @org.springframework.data.jpa.repository.Query("""
            select n.prenotazione.id from Notifica n
            where n.tipo = :tipo and n.prenotazione.id in :prenotazioneIds
            """)
    List<Long> findPrenotazioneIdsConNotifica(
            @org.springframework.data.repository.query.Param("tipo") TipoNotifica tipo,
            @org.springframework.data.repository.query.Param("prenotazioneIds") Collection<Long> prenotazioneIds);

    /**
     * Inviti ancora pendenti su una prenotazione: quando l'utente recensisce, la notifica
     * che lo invitava a farlo non ha piu' motivo di restare in elenco.
     */
    List<Notifica> findByPrenotazione_IdAndTipo(Long prenotazioneId, TipoNotifica tipo);
}
