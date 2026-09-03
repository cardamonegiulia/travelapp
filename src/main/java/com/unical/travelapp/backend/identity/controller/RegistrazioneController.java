package com.unical.travelapp.backend.identity.controller;

import com.unical.travelapp.backend.identity.dto.RegistrazioneRequest;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.service.RegistrazioneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrazioneController {

    private final RegistrazioneService registrazioneService;

    public RegistrazioneController(RegistrazioneService registrazioneService) {
        this.registrazioneService = registrazioneService;
    }

    @PostMapping("/registrazione")
    @Operation(
            summary = "Registra un nuovo utente",
            description = "Crea l'utente su Keycloak, gli assegna il ruolo realm scelto (VIAGGIATORE o "
                    + "ORGANIZZATORE) e salva il record locale con lo stesso ruolo. Endpoint pubblico: "
                    + "non richiede token. ADMIN non è richiedibile da qui."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utente registrato"),
            @ApiResponse(responseCode = "400", description = "Dati non validi o ruolo non ammesso", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email già registrata", content = @Content),
            @ApiResponse(responseCode = "503", description = "Keycloak non disponibile o ruolo realm non configurato", content = @Content)
    })
    public ResponseEntity<UtenteResponseDto> registra(@Valid @RequestBody RegistrazioneRequest richiesta) {
        UtenteResponseDto registrato = registrazioneService.registra(richiesta);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrato);
    }
}
