package com.unical.travelapp.backend.validation;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.catalog.dto.GiornoProgrammaDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @Test
    void richiestaPrenotazioneSenzaNumeroPartecipantiNonEValida() {
        CreaPrenotazioneRequest req = new CreaPrenotazioneRequest();
        req.setDisponibilitaItinerarioId(1L);

        Set<ConstraintViolation<CreaPrenotazioneRequest>> violazioni = VALIDATOR.validate(req);

        assertThat(violazioni).anyMatch(v -> v.getPropertyPath().toString().equals("numeroPartecipanti"));
    }

    @Test
    void richiestaPrenotazioneConNumeroPartecipantiNegativoNonEValida() {
        CreaPrenotazioneRequest req = new CreaPrenotazioneRequest();
        req.setNumeroPartecipanti(-1);
        req.setDisponibilitaItinerarioId(1L);

        Set<ConstraintViolation<CreaPrenotazioneRequest>> violazioni = VALIDATOR.validate(req);

        assertThat(violazioni).anyMatch(v -> v.getPropertyPath().toString().equals("numeroPartecipanti"));
    }

    @Test
    void richiestaItinerarioSenzaTitoloNonEValida() {
        ItinerarioRequestDTO dto = itinerarioValido();
        dto.setTitolo(null);

        Set<ConstraintViolation<ItinerarioRequestDTO>> violazioni = VALIDATOR.validate(dto);

        assertThat(violazioni).anyMatch(v -> v.getPropertyPath().toString().equals("titolo"));
    }

    @Test
    void richiestaItinerarioConPrezzoNegativoNonEValida() {
        ItinerarioRequestDTO dto = itinerarioValido();
        dto.setPrezzoBase(BigDecimal.valueOf(-10));

        Set<ConstraintViolation<ItinerarioRequestDTO>> violazioni = VALIDATOR.validate(dto);

        assertThat(violazioni).anyMatch(v -> v.getPropertyPath().toString().equals("prezzoBase"));
    }

    @Test
    void richiestaItinerarioSenzaProgrammaNonEValida() {
        ItinerarioRequestDTO dto = itinerarioValido();
        dto.setProgramma(List.of());

        Set<ConstraintViolation<ItinerarioRequestDTO>> violazioni = VALIDATOR.validate(dto);

        assertThat(violazioni).anyMatch(v -> v.getPropertyPath().toString().equals("programma"));
    }

    @Test
    void richiestaItinerarioConGiornataSenzaDescrizioneNonEValida() {
        ItinerarioRequestDTO dto = itinerarioValido();
        dto.setProgramma(List.of(giorno("Arrivo e check-in", "")));

        Set<ConstraintViolation<ItinerarioRequestDTO>> violazioni = VALIDATOR.validate(dto);

        assertThat(violazioni)
                .anyMatch(v -> v.getPropertyPath().toString().equals("programma[0].descrizione"));
    }

    @Test
    void richiestaItinerarioCompletaEValida() {
        assertThat(VALIDATOR.validate(itinerarioValido())).isEmpty();
    }

    private static ItinerarioRequestDTO itinerarioValido() {
        ItinerarioRequestDTO dto = new ItinerarioRequestDTO();
        dto.setTitolo("Tour di Roma");
        dto.setDestinazionePrincipale("Roma");
        dto.setPrezzoBase(BigDecimal.valueOf(199.90));
        dto.setDurataGiorni(3);
        dto.setMaxPartecipanti(10);
        dto.setProgramma(List.of(giorno("Arrivo e check-in", "Accoglienza e briefing iniziale.")));
        return dto;
    }

    private static GiornoProgrammaDTO giorno(String titolo, String descrizione) {
        GiornoProgrammaDTO giorno = new GiornoProgrammaDTO();
        giorno.setTitolo(titolo);
        giorno.setDescrizione(descrizione);
        return giorno;
    }
}
