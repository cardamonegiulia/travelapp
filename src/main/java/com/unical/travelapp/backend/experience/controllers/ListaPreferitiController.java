package com.unical.travelapp.backend.experience.controllers;

import com.unical.travelapp.backend.experience.models.DTO.CondivisioneRequest;
import com.unical.travelapp.backend.experience.models.DTO.ListaPreferitiDTO;
import com.unical.travelapp.backend.experience.models.DTO.ListaPreferitiRequest;
import com.unical.travelapp.backend.experience.models.DTO.PreferitoItinerarioRequest;
import com.unical.travelapp.backend.experience.services.ListaPreferitiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Liste di itinerari preferiti del viaggiatore: private, oppure condivise con utenti
 * specifici.
 *
 * <p>Nessun endpoint accetta l'id del proprietario dal client: e' sempre l'utente del
 * token. Chi puo' leggere o modificare una lista lo decide {@link ListaPreferitiService}.
 */
@RestController
@RequestMapping("/api/preferiti")
@Tag(name = "Gestione Preferiti", description = "Liste di itinerari preferiti, private o condivise con utenti specifici")
@SecurityRequirement(name = "bearerAuth")
public class ListaPreferitiController {

    private final ListaPreferitiService service;

    public ListaPreferitiController(ListaPreferitiService service) {
        this.service = service;
    }

    // --- elenchi ----------------------------------------------------------------------

    @GetMapping
    @Operation(
            summary = "Le liste di preferiti dell'utente",
            description = "Accessibile da qualsiasi utente autenticato. Restituisce le liste di cui l'utente "
                    + "loggato è proprietario, private e condivise insieme, senza gli itinerari: per quelli "
                    + "serve il dettaglio della singola lista. Lista vuota se non ne ha ancora create."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Elenco delle liste restituito con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<List<ListaPreferitiDTO>> getMieListe() {
        return ResponseEntity.ok(service.getMieListe());
    }

    @GetMapping("/condivise-con-me")
    @Operation(
            summary = "Le liste che altri utenti hanno condiviso con me",
            description = "Restituisce le liste di altri viaggiatori in cui l'utente loggato compare fra i "
                    + "destinatari. Sono in sola lettura."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Elenco restituito con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<List<ListaPreferitiDTO>> getListeCondiviseConMe() {
        return ResponseEntity.ok(service.getListeCondiviseConMe());
    }

    @GetMapping("/{listaId}")
    @Operation(
            summary = "Dettaglio di una lista, con i suoi itinerari",
            description = "Accessibile al proprietario e agli utenti con cui la lista è condivisa. "
                    + "Per tutti gli altri la lista non esiste: 404."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista restituita con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Lista inesistente o non accessibile")
    })
    public ResponseEntity<ListaPreferitiDTO> getLista(@PathVariable Long listaId) {
        return ResponseEntity.ok(service.getLista(listaId));
    }

    // --- ciclo di vita della lista ----------------------------------------------------

    @PostMapping
    @Operation(
            summary = "Crea una nuova lista di preferiti",
            description = "Riceve nome e visibilità (PRIVATA di default, CONDIVISA per poterla poi condividere "
                    + "con utenti specifici). Il proprietario è sempre l'utente loggato."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lista creata con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi o nome già usato"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido")
    })
    public ResponseEntity<ListaPreferitiDTO> creaLista(@Valid @RequestBody ListaPreferitiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creaLista(request));
    }

    @PutMapping("/{listaId}")
    @Operation(
            summary = "Rinomina una lista o ne cambia la visibilità",
            description = "Solo il proprietario. Riportare la lista a PRIVATA revoca tutte le condivisioni."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista aggiornata con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi o nome già usato"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "La lista non è dell'utente loggato"),
            @ApiResponse(responseCode = "404", description = "Lista inesistente o non accessibile")
    })
    public ResponseEntity<ListaPreferitiDTO> aggiornaLista(@PathVariable Long listaId,
                                                           @Valid @RequestBody ListaPreferitiRequest request) {
        return ResponseEntity.ok(service.aggiornaLista(listaId, request));
    }

    @DeleteMapping("/{listaId}")
    @Operation(
            summary = "Elimina una lista di preferiti",
            description = "Solo il proprietario. Gli itinerari non vengono toccati: sparisce solo la lista."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lista eliminata con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "La lista non è dell'utente loggato"),
            @ApiResponse(responseCode = "404", description = "Lista inesistente o non accessibile")
    })
    public ResponseEntity<Void> eliminaLista(@PathVariable Long listaId) {
        service.eliminaLista(listaId);
        return ResponseEntity.noContent().build();
    }

    // --- itinerari dentro una lista ---------------------------------------------------

    @PostMapping("/{listaId}/itinerari")
    @Operation(
            summary = "Aggiunge un itinerario a una lista",
            description = "Solo il proprietario. Se l'itinerario è già nella lista la richiesta non lo duplica."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Itinerario aggiunto con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "La lista non è dell'utente loggato"),
            @ApiResponse(responseCode = "404", description = "Lista o itinerario non trovati")
    })
    public ResponseEntity<ListaPreferitiDTO> aggiungiItinerario(@PathVariable Long listaId,
                                                                @Valid @RequestBody PreferitoItinerarioRequest request) {
        ListaPreferitiDTO aggiornata = service.aggiungiItinerario(listaId, request.getItinerarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(aggiornata);
    }

    @DeleteMapping("/{listaId}/itinerari/{itinerarioId}")
    @Operation(
            summary = "Rimuove un itinerario da una lista",
            description = "Solo il proprietario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itinerario rimosso con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "La lista non è dell'utente loggato"),
            @ApiResponse(responseCode = "404", description = "Lista o itinerario non trovati")
    })
    public ResponseEntity<ListaPreferitiDTO> rimuoviItinerario(@PathVariable Long listaId,
                                                               @PathVariable Long itinerarioId) {
        return ResponseEntity.ok(service.rimuoviItinerario(listaId, itinerarioId));
    }

    // --- scorciatoia "cuore" ----------------------------------------------------------
    // Il gesto rapido sulla scheda di un itinerario non può fermarsi a chiedere in quale
    // lista salvare: queste due rotte lavorano sulla lista predefinita "I miei preferiti",
    // che viene creata privata alla prima occorrenza.

    @PostMapping("/itinerari")
    @Operation(
            summary = "Salva un itinerario senza scegliere la lista",
            description = "Aggiunge l'itinerario alla lista predefinita \"I miei preferiti\" dell'utente, "
                    + "creandola privata se non esiste ancora."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Itinerario aggiunto ai preferiti con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Itinerario non trovato")
    })
    public ResponseEntity<ListaPreferitiDTO> aggiungiAiPreferiti(
            @Valid @RequestBody PreferitoItinerarioRequest request) {
        ListaPreferitiDTO aggiornata = service.aggiungiAllaListaPredefinita(request.getItinerarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(aggiornata);
    }

    @DeleteMapping("/itinerari/{itinerarioId}")
    @Operation(
            summary = "Toglie un itinerario dai preferiti",
            description = "Lo rimuove da tutte le liste dell'utente loggato. Le liste degli altri utenti, "
                    + "comprese quelle condivise con lui, non vengono toccate."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Itinerario rimosso dai preferiti"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Itinerario non trovato")
    })
    public ResponseEntity<Void> rimuoviDaiPreferiti(@PathVariable Long itinerarioId) {
        service.rimuoviDaTutteLeMieListe(itinerarioId);
        return ResponseEntity.noContent().build();
    }

    // --- condivisione -----------------------------------------------------------------

    @PostMapping("/{listaId}/condivisioni")
    @Operation(
            summary = "Condivide la lista con un utente specifico",
            description = "Solo il proprietario. L'utente si indica per id oppure per email e ottiene la sola "
                    + "lettura. La lista diventa CONDIVISA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista condivisa con successo"),
            @ApiResponse(responseCode = "400", description = "Destinatario non indicato o coincidente con il proprietario"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "La lista non è dell'utente loggato"),
            @ApiResponse(responseCode = "404", description = "Lista o utente destinatario non trovati")
    })
    public ResponseEntity<ListaPreferitiDTO> condividi(@PathVariable Long listaId,
                                                       @Valid @RequestBody CondivisioneRequest request) {
        return ResponseEntity.ok(service.condividiCon(listaId, request));
    }

    @DeleteMapping("/{listaId}/condivisioni/{utenteId}")
    @Operation(
            summary = "Revoca a un utente l'accesso alla lista",
            description = "Solo il proprietario. La visibilità dichiarata della lista non cambia: per togliere "
                    + "l'accesso a tutti in un colpo solo si riporta la lista a PRIVATA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Condivisione revocata con successo"),
            @ApiResponse(responseCode = "401", description = "Token JWT mancante o non valido"),
            @ApiResponse(responseCode = "403", description = "La lista non è dell'utente loggato"),
            @ApiResponse(responseCode = "404", description = "Lista inesistente o non accessibile")
    })
    public ResponseEntity<ListaPreferitiDTO> revocaCondivisione(@PathVariable Long listaId,
                                                                @PathVariable Long utenteId) {
        return ResponseEntity.ok(service.revocaCondivisione(listaId, utenteId));
    }
}
