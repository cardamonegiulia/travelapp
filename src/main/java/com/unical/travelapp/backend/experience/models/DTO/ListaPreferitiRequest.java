package com.unical.travelapp.backend.experience.models.DTO;

import com.unical.travelapp.backend.experience.models.VisibilitaListaPreferiti;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Creazione o modifica di una lista di preferiti: il client decide solo nome e
 * visibilita'. Il proprietario e' sempre l'utente del token, mai un campo del payload.
 */
@Data
public class ListaPreferitiRequest {

    @NotBlank(message = "il nome della lista è obbligatorio")
    @Size(max = 80, message = "il nome della lista può avere al massimo 80 caratteri")
    @Schema(description = "Nome della lista", example = "Viaggi d'estate")
    private String nome;

    // Opzionale: una lista nasce privata se non viene detto altro, cosi' la scelta piu'
    // riservata e' anche quella di default.
    @Schema(description = "PRIVATA (default) oppure CONDIVISA", example = "PRIVATA")
    private VisibilitaListaPreferiti visibilita;

    public VisibilitaListaPreferiti visibilitaRichiesta() {
        return visibilita == null ? VisibilitaListaPreferiti.PRIVATA : visibilita;
    }
}
