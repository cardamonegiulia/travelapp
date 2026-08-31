package com.unical.travelapp.backend.experience.models.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO di request: niente utenteId, e' sempre ricavato dal token (vedi RecensioneService)
@Data
public class RecensioneRequest {

    // Obbligatorio: si recensisce un viaggio che si e' prenotato e concluso, non un
    // itinerario qualsiasi del catalogo. E' da qui che il service ricava sia l'autore
    // legittimo sia l'itinerario da collegare.
    @NotNull(message = "prenotazioneId è obbligatorio: si può recensire solo un viaggio prenotato")
    @Positive(message = "prenotazioneId non valido")
    private Long prenotazioneId;

    // Facoltativo e ridondante: se valorizzato deve coincidere con l'itinerario della
    // prenotazione. Resta accettato per non rompere i client che lo inviano gia'.
    @Positive(message = "itinerarioId non valido")
    private Long itinerarioId;

    @NotNull(message = "La valutazione è obbligatoria")
    @Min(value = 1, message = "Il voto deve essere compreso tra 1 e 5")
    @Max(value = 5, message = "Il voto deve essere compreso tra 1 e 5")
    private Integer votazione;

    // Il commento e' facoltativo: si puo' lasciare solo il voto in stelle.
    @Size(max = 2000, message = "Il commento non può superare i 2000 caratteri")
    private String comm;
}
