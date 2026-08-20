package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.booking.entity.Prenotazione;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.common.audit.Auditable;
import com.unical.travelapp.backend.identity.entity.Utente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.BatchSize;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "recensioni")
public class Recensione extends Auditable {

    @Id
    @Column(name = "recensione_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "ID generato automaticamente", example = "123")
    private long Id;

    @ManyToOne
    @JoinColumn(name = "prenotazione_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "prenotazione a cui appartiene la recensione")
    private Prenotazione prenotazione;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "autore_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "utente che ha scritto la recensione")
    private Utente utente;

    @Schema(description = "votazione del viaggio", example = "3")
    private int voto;

    @Schema(description = "testo della recensione", example = "il viaggio e' stato molto divertente e interessante")
    private String commento;

    @ManyToOne
    @JoinColumn(name = "itinerario_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "itinerario a cui appartiene recensione")
    private Itinerario itinerario;

    // Relazione unidirezionale: la chiave esterna sta su "immagini", non qui. Cosi'
    // cancellare una singola immagine non lascia mai riferimenti pendenti su questa tabella.
    // BatchSize: senza, l'elenco paginato delle recensioni farebbe una query per ogni riga
    // solo per caricarne le foto (problema N+1); con il batch ne bastano una o due.
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "recensione_id")
    @OrderBy("id ASC")
    @BatchSize(size = 30)
    @Schema(description = "foto allegate alla recensione")
    private List<Immagine> immagini = new ArrayList<>();
}
