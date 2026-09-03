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

    /**
     * Ultimo istante utile per prenotare questa partenza: il termine fissato
     * dall'organizzatore, o la partenza stessa quando non ne ha fissato uno.
     */
    public LocalDateTime terminePrenotazioni() {
        return dataLimitePrenotazione != null ? dataLimitePrenotazione : dataInizio;
    }

    /**
     * true finche' il termine non e' passato. Quando diventa false la partenza non va
     * piu' proposta al viaggiatore: non e' una data "chiusa" da mostrare barrata, e' una
     * data che sparisce dalla scheda.
     */
    public boolean prenotabileAl(LocalDateTime istante) {
        LocalDateTime termine = terminePrenotazioni();
        return termine != null && !termine.isBefore(istante);
    }
}
