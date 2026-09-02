package com.unical.travelapp.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Una giornata del programma dell'itinerario: "Giorno 1 - Arrivo e check-in", con la
 * descrizione di cosa si fa.
 *
 * <p>Non e' una {@link Tappa}: la tappa e' una citta' del viaggio e porta con se' le
 * attivita' prenotabili, questo e' il racconto giorno per giorno che il viaggiatore legge
 * nella scheda dell'itinerario prima di prenotare.
 */
@Entity
@Table(name = "programma_giorni")
@Data
// L'itinerario e' escluso da equals/hashCode/toString: la relazione e' bidirezionale e
// includerlo farebbe ricorsione infinita (l'itinerario stampa il programma, il programma
// l'itinerario).
@EqualsAndHashCode(exclude = "itinerario")
@ToString(exclude = "itinerario")
public class GiornoProgramma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "itinerario_id")
    private Itinerario itinerario;

    // Progressivo a partire da 1: e' la posizione nel programma, non una data.
    private Integer giorno;

    private String titolo;

    @Column(columnDefinition = "TEXT")
    private String descrizione;
}
