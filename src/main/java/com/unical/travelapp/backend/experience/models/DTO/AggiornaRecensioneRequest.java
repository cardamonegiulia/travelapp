package com.unical.travelapp.backend.experience.models.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Modifica di una recensione gia' scritta: si cambiano solo voto e commento.
 *
 * <p>DTO separato da {@link RecensioneRequest} di proposito: la prenotazione a cui la
 * recensione si riferisce e' decisa alla creazione e non e' un campo che il client possa
 * rimettere in discussione con una PUT.
 */
@Data
public class AggiornaRecensioneRequest {

    @NotNull(message = "La valutazione è obbligatoria")
    @Min(value = 1, message = "Il voto deve essere compreso tra 1 e 5")
    @Max(value = 5, message = "Il voto deve essere compreso tra 1 e 5")
    private Integer votazione;

    // Facoltativo: si puo' lasciare (o togliere) il solo voto in stelle.
    @Size(max = 2000, message = "Il commento non può superare i 2000 caratteri")
    private String comm;
}
