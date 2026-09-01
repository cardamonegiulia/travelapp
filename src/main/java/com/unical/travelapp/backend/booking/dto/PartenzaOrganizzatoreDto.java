package com.unical.travelapp.backend.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Una partenza di un itinerario vista da chi l'ha organizzata: le date, i posti ancora
 * liberi e quanto e' stato venduto.
 *
 * <p>Non si riusa {@code DisponibilitaItinerarioDTO} perche' quello e' la vista di chi
 * deve prenotare: qui servono i numeri delle prenotazioni, che al viaggiatore non
 * riguardano.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartenzaOrganizzatoreDto {

    private Long disponibilitaId;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private Integer postiDisponibili;

    /** Prenotazioni attive su questa partenza (le cancellate non contano). */
    private long numeroPrenotazioni;

    /** Somma dei partecipanti delle prenotazioni attive. */
    private long partecipantiTotali;
}
