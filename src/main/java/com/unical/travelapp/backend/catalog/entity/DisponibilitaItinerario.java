package com.unical.travelapp.backend.catalog.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Entity
@Table(name = "disponibilita_itinerari")
@Data
public class DisponibilitaItinerario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    @ManyToOne
    @JoinColumn(name = "itinerario_id")
    private Itinerario itinerario;
    @Column(name = "data_inizio")
    private LocalDateTime dataInizio;
    @Column(name = "data_fine")
    private LocalDateTime dataFine;
    @Column(name = "data_limite_prenotazione")
    private LocalDateTime dataLimitePrenotazione;
    @Column(name = "posti_disponibili")
    private Integer postiDisponibili;

    public LocalDateTime terminePrenotazioni() {
        return dataLimitePrenotazione != null ? dataLimitePrenotazione : dataInizio;
    }

    public boolean prenotabileAl(LocalDateTime istante) {
        LocalDateTime termine = terminePrenotazioni();
        return termine != null && !termine.isBefore(istante);
    }
}
