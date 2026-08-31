package com.unical.travelapp.backend.catalog.dto;

import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItinerarioDTO {

    private Long id;
    private Long organizzatoreId;
    private String titolo;
    private String descrizione;
    private String destinazionePrincipale;
    private BigDecimal prezzoBase;
    private Integer durataGiorni;

    // Periodo del viaggio: ricavato dalla disponibilità dell'itinerario, quindi assente
    // finché l'organizzatore non ne ha indicata una.
    private LocalDate dataInizio;
    private LocalDate dataFine;

    // Termine ultimo per prenotare la prima partenza: assente se non ne e' stato indicato uno.
    private LocalDate dataLimitePrenotazione;

    private Integer maxPartecipanti;
    private String stato;

    /**
     * Voto medio delle recensioni ricevute, {@code null} se non ce n'e' nessuna: zero non e'
     * un voto assegnabile, e restituirlo farebbe disegnare cinque stelle vuote al posto di
     * "nessuna recensione".
     */
    private Double mediaVoti;

    /** Su quante recensioni e' calcolata la media. */
    private long numeroRecensioni;

    /**
     * true se l'itinerario ha almeno una partenza ancora prenotabile.
     *
     * <p>Un itinerario senza date NON sparisce dalla bacheca: resta visibile e il client lo
     * segnala con un'etichetta, perche' l'organizzatore puo' aggiungerne di nuove in
     * qualsiasi momento. Sparisce solo quando l'organizzatore lo elimina.
     */
    private boolean dateDisponibili;

    // galleria dell'itinerario: la prima immagine e' la copertina.
    // Sempre presente, eventualmente vuota.
    private List<ImmagineResponse> immagini = new ArrayList<>();
}