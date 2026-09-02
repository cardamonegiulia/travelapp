package com.unical.travelapp.backend.catalog.entity;

import com.unical.travelapp.backend.common.audit.Auditable;
import com.unical.travelapp.backend.experience.models.Immagine;
import com.unical.travelapp.backend.experience.models.Recensione;
import org.hibernate.annotations.BatchSize;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.unical.travelapp.backend.identity.entity.Utente;

@Entity
@Table(name = "itinerari")
@Data
@EqualsAndHashCode(callSuper = false)
public class Itinerario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organizzatore_id")
    private Utente organizzatore;

    private String titolo;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    @Column(name = "destinazione_principale")
    private String destinazionePrincipale;

    @Column(name = "prezzo_base")
    private BigDecimal prezzoBase;

    @Column(name = "durata_giorni")
    private Integer durataGiorni;

    @Column(name = "max_partecipanti")
    private Integer maxPartecipanti;

    private String stato;

    @OneToMany(mappedBy = "itinerario", cascade = CascadeType.ALL)
    private List<Tappa> tappe;

    @OneToMany(mappedBy = "itinerario", cascade = CascadeType.ALL)
    private List<DisponibilitaItinerario> disponibilita;

    // Programma giorno per giorno, mostrato nella scheda dell'itinerario. E' parte della
    // descrizione del viaggio, quindi vive e muore con l'itinerario (orphanRemoval): un
    // aggiornamento riscrive l'elenco e le giornate rimosse spariscono davvero.
    @OneToMany(mappedBy = "itinerario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("giorno ASC")
    @BatchSize(size = 30)
    private List<GiornoProgramma> programma = new ArrayList<>();

    @OneToMany(mappedBy = "itinerario", cascade = CascadeType.ALL)
    private List<Recensione> recensioni;

    // Galleria del viaggio; per convenzione la prima immagine e' quella di copertina
    // (@OrderBy garantisce che l'ordine sia sempre lo stesso, altrimenti la "prima" foto
    // cambierebbe a seconda di come il database restituisce le righe).
    // La chiave esterna sta su "immagini": vedi il commento in Recensione.
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerario_id")
    @OrderBy("id ASC")
    @BatchSize(size = 30)
    private List<Immagine> immagini = new ArrayList<>();
}