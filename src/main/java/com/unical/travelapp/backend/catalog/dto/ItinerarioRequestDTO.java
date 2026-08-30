package com.unical.travelapp.backend.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// DTO di request: niente id/organizzatoreId/stato, sono gestiti dal server (mai dal client)
@Data
public class ItinerarioRequestDTO {

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 150, message = "Il titolo non può superare i 150 caratteri")
    private String titolo;

    @Size(max = 5000, message = "La descrizione non può superare i 5000 caratteri")
    private String descrizione;

    @NotBlank(message = "La destinazione principale è obbligatoria")
    @Size(max = 150, message = "La destinazione non può superare i 150 caratteri")
    private String destinazionePrincipale;

    @NotNull(message = "Il prezzo base è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "Il prezzo base deve essere positivo")
    private BigDecimal prezzoBase;

    // Non più obbligatoria: se arriva il periodo del viaggio la durata la calcola il server
    // (vedi durataDalPeriodo). Resta accettata da sola per i client che non inviano le date.
    @Positive(message = "La durata deve essere positiva")
    private Integer durataGiorni;

    @FutureOrPresent(message = "La data di inizio non può essere nel passato")
    private LocalDate dataInizio;

    private LocalDate dataFine;

    // Termine ultimo per prenotare: facoltativo, se manca si prenota fino alla partenza.
    @FutureOrPresent(message = "La data limite per le prenotazioni non può essere nel passato")
    private LocalDate dataLimitePrenotazione;

    @NotNull(message = "Il numero massimo di partecipanti è obbligatorio")
    @Positive(message = "Il numero massimo di partecipanti deve essere positivo")
    private Integer maxPartecipanti;

    // O si inviano entrambe le date del viaggio, o nessuna: una sola non descrive un periodo.
    @JsonIgnore
    @AssertTrue(message = "Indicare sia la data di inizio sia quella di fine, con la fine non precedente all'inizio")
    public boolean isPeriodoCoerente() {
        if (dataInizio == null && dataFine == null) {
            return true;
        }
        return dataInizio != null && dataFine != null && !dataFine.isBefore(dataInizio);
    }

    // Un termine per le prenotazioni ha senso solo con un periodo, e non oltre la partenza.
    @JsonIgnore
    @AssertTrue(message = "La data limite per le prenotazioni richiede le date del viaggio e non può essere successiva alla partenza")
    public boolean isLimitePrenotazioniCoerente() {
        if (dataLimitePrenotazione == null) {
            return true;
        }
        return dataInizio != null && !dataLimitePrenotazione.isAfter(dataInizio);
    }

    // La durata deve poter essere determinata: dalle date oppure dal campo esplicito.
    @JsonIgnore
    @AssertTrue(message = "Indicare le date del viaggio oppure la durata in giorni")
    public boolean isDurataDeterminabile() {
        return dataInizio != null || durataGiorni != null;
    }

    // Durata ricavata dal periodo, estremi inclusi: null se il periodo non è stato indicato.
    public Integer durataDalPeriodo() {
        if (!isPeriodoCoerente() || dataInizio == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(dataInizio, dataFine) + 1;
    }
}
