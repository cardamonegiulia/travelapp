package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.common.audit.Auditable;
import com.unical.travelapp.backend.identity.entity.Utente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Notifica in-app destinata a un singolo utente.
 *
 * <p>Unica infrastruttura di notifica del progetto: i nuovi tipi si aggiungono a
 * {@link TipoNotifica} e riusano questa tabella invece di crearne una propria.
 *
 * <p>{@link #prenotazione} e {@link #itinerario} sono i riferimenti su cui il client
 * costruisce l'azione diretta (per l'invito a recensire: aprire il form della recensione).
 * Sono nullable perche' non tutti i tipi di notifica riguardano una prenotazione, e hanno
 * ON DELETE CASCADE: cancellata la prenotazione, la notifica che la nomina non ha piu' senso.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "notifiche")
public class Notifica extends Auditable {

    @Id
    @Column(name = "notifica_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID generato automaticamente", example = "123")
    private Long id;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "destinatario_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "utente a cui e' destinata la notifica")
    private Utente destinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 40, nullable = false)
    @Schema(description = "tipo di notifica", example = "INVITO_RECENSIONE")
    private TipoNotifica tipo;

    @Column(name = "titolo", length = 120)
    @Schema(description = "titolo breve mostrato in elenco")
    private String titolo;

    @Column(name = "messaggio", length = 500)
    @Schema(description = "testo della notifica")
    private String messaggio;

    @Column(name = "letta", nullable = false)
    @Schema(description = "true quando l'utente l'ha aperta")
    private boolean letta = false;

    @ManyToOne
    @JoinColumn(name = "prenotazione_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "prenotazione a cui si riferisce la notifica, se ce n'e' una")
    private Prenotazione prenotazione;

    @ManyToOne
    @JoinColumn(name = "itinerario_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "itinerario a cui si riferisce la notifica, se ce n'e' uno")
    private Itinerario itinerario;
}
