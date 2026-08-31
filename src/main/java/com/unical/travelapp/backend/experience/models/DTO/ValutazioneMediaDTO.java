package com.unical.travelapp.backend.experience.models.DTO;

/**
 * Voto medio di un itinerario e numero di recensioni su cui e' calcolato.
 *
 * <p>{@code media} e' {@code null} quando non c'e' nessuna recensione: e' diverso da zero,
 * che non e' un voto assegnabile, e permette al client di mostrare "nessuna recensione"
 * invece di cinque stelle vuote.
 */
public record ValutazioneMediaDTO(Double media, long numero) {

    public static final ValutazioneMediaDTO NESSUNA = new ValutazioneMediaDTO(null, 0L);
}
