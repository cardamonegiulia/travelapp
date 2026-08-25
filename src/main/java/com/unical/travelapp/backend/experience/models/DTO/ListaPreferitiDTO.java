package com.unical.travelapp.backend.experience.models.DTO;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.experience.models.VisibilitaListaPreferiti;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Una lista di itinerari preferiti come viene restituita al client.
 *
 * <p>Vale sia per le liste dell'utente sia per quelle che qualcun altro ha condiviso con
 * lui: {@link #proprietaria} dice da che parte si trova chi sta leggendo, cosi' il client
 * sa se mostrare o meno i comandi di modifica.
 */
@Data
@Schema(description = "Lista di itinerari preferiti, privata o condivisa con utenti specifici")
public class ListaPreferitiDTO {

    private Long id;

    @Schema(description = "Nome scelto dal proprietario", example = "Viaggi d'estate")
    private String nome;

    @Schema(description = "PRIVATA oppure CONDIVISA")
    private VisibilitaListaPreferiti visibilita;

    @Schema(description = "ID del viaggiatore proprietario della lista")
    private Long proprietarioId;

    @Schema(description = "Nome e cognome del proprietario, per le liste condivise con l'utente")
    private String proprietarioNome;

    @Schema(description = "true se chi sta leggendo e' il proprietario e puo' quindi modificarla")
    private boolean proprietaria;

    @Schema(description = "Numero di itinerari salvati, utile per gli elenchi compatti")
    private int numeroItinerari;

    @Schema(description = "Itinerari salvati. Valorizzato solo nel dettaglio della lista")
    private List<ItinerarioDTO> itinerari = new ArrayList<>();

    // Chi puo' vedere con quali utenti una lista e' condivisa e' il proprietario, e basta:
    // il mapper lascia questa lista vuota per i destinatari, che non devono sapere chi
    // altro legge la stessa lista.
    @Schema(description = "Utenti con cui la lista e' condivisa. Visibile solo al proprietario")
    private List<UtenteCondivisioneDTO> destinatari = new ArrayList<>();
}
