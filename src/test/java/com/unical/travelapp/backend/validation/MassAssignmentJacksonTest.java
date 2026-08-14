package com.unical.travelapp.backend.validation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Prova che con FAIL_ON_UNKNOWN_PROPERTIES=true un campo "di sistema" iniettato nel payload
// (es. organizzatoreId, che non esiste nel DTO di request) fa fallire la deserializzazione
// invece di essere silenziosamente accettato (protezione da mass assignment).
class MassAssignmentJacksonTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Test
    void rifiutaOrganizzatoreIdIniettatoNelPayloadDiCreazioneItinerario() {
        String payloadConCampoDiSistema = """
                {
                  "titolo": "Tour di Roma",
                  "destinazionePrincipale": "Roma",
                  "prezzoBase": 199.90,
                  "durataGiorni": 3,
                  "maxPartecipanti": 10,
                  "organizzatoreId": 999
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(payloadConCampoDiSistema, ItinerarioRequestDTO.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    @Test
    void accettaPayloadSenzaCampiDiSistema() throws Exception {
        String payloadValido = """
                {
                  "titolo": "Tour di Roma",
                  "destinazionePrincipale": "Roma",
                  "prezzoBase": 199.90,
                  "durataGiorni": 3,
                  "maxPartecipanti": 10
                }
                """;

        ItinerarioRequestDTO dto = objectMapper.readValue(payloadValido, ItinerarioRequestDTO.class);

        org.assertj.core.api.Assertions.assertThat(dto.getTitolo()).isEqualTo("Tour di Roma");
    }
}
