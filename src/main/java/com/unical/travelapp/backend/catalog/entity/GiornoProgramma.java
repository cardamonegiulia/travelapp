package com.unical.travelapp.backend.catalog.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
@Entity
@Table(name = "programma_giorni")
@Data
@EqualsAndHashCode(exclude = "itinerario")
@ToString(exclude = "itinerario")
public class GiornoProgramma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "itinerario_id")
    private Itinerario itinerario;
    private Integer giorno;
    private String titolo;
    @Column(columnDefinition = "TEXT")
    private String descrizione;
}
