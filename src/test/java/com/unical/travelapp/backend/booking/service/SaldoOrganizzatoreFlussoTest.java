package com.unical.travelapp.backend.booking.service;

import com.unical.travelapp.backend.booking.dto.CreaPrenotazioneRequest;
import com.unical.travelapp.backend.catalog.entity.DisponibilitaItinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SaldoOrganizzatoreFlussoTest extends SecurityIntegrationTestBase {

    private Utente organizzatore;
    private DisponibilitaItinerario partenza;

    @BeforeEach
    void datiDiBase() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        partenza = disponibilita(itinerario(organizzatore), 10);
    }

    @Test
    void ilSaldoSeguePrenotazionePagamentoEAnnullamento() throws Exception {
        assertThat(saldo()).isEqualByComparingTo("0");

        Long prenotazioneId = prenotaItinerario(SUB_UTENTE_A, 2);
        assertThat(saldo())
                .as("una prenotazione non ancora pagata non e' un incasso")
                .isEqualByComparingTo("0");

        paga(SUB_UTENTE_A, prenotazioneId);
        assertThat(saldo()).isEqualByComparingTo("200.00");

        annulla(SUB_UTENTE_A, prenotazioneId);
        assertThat(saldo())
                .as("chi annulla viene rimborsato: l'importo esce dal saldo")
                .isEqualByComparingTo("0");
    }

    @Test
    void restaNelSaldoSoloChiNonHaAnnullato() throws Exception {
        Long primaPrenotazione = prenotaItinerario(SUB_UTENTE_A, 2);
        paga(SUB_UTENTE_A, primaPrenotazione);

        Long secondaPrenotazione = prenotaItinerario(SUB_UTENTE_B, 1);
        paga(SUB_UTENTE_B, secondaPrenotazione);

        assertThat(saldo()).isEqualByComparingTo("300.00");

        annulla(SUB_UTENTE_A, primaPrenotazione);

        assertThat(saldo()).isEqualByComparingTo("100.00");
    }

    @Test
    void ancheLAnnullamentoDiUnaSingolaAttivitaScalaIlSaldo() throws Exception {
        Long sessioneId = sessione(organizzatore, 10).getId();

        CreaPrenotazioneRequest richiesta = new CreaPrenotazioneRequest();
        richiesta.setSessioneSingolaAttivitaId(sessioneId);
        richiesta.setNumeroPartecipanti(2);

        Long prenotazioneId = prenota(SUB_UTENTE_A, richiesta);
        paga(SUB_UTENTE_A, prenotazioneId);
        assertThat(saldo()).isEqualByComparingTo("60.00");

        annulla(SUB_UTENTE_A, prenotazioneId);
        assertThat(saldo()).isEqualByComparingTo("0");
    }

    @Test
    void chiAnnullaSenzaAverPagatoNonSpostaIlSaldo() throws Exception {
        Long pagata = prenotaItinerario(SUB_UTENTE_A, 1);
        paga(SUB_UTENTE_A, pagata);

        Long maiPagata = prenotaItinerario(SUB_UTENTE_B, 1);
        annulla(SUB_UTENTE_B, maiPagata);

        assertThat(saldo()).isEqualByComparingTo("100.00");
    }

    private Long prenotaItinerario(String subject, int partecipanti) throws Exception {
        CreaPrenotazioneRequest richiesta = new CreaPrenotazioneRequest();
        richiesta.setDisponibilitaItinerarioId(partenza.getId());
        richiesta.setNumeroPartecipanti(partecipanti);
        return prenota(subject, richiesta);
    }

    private Long prenota(String subject, CreaPrenotazioneRequest richiesta) throws Exception {
        String corpo = mockMvc.perform(post("/api/prenotazioni")
                        .with(TestJwt.conRuoliRealm(subject, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(richiesta)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(corpo).get("id").asLong();
    }

    private void paga(String subject, Long prenotazioneId) throws Exception {
        mockMvc.perform(post("/api/pagamenti/prenotazioni/" + prenotazioneId + "/paga")
                        .with(TestJwt.conRuoliRealm(subject, "VIAGGIATORE")))
                .andExpect(status().isOk());
    }

    private void annulla(String subject, Long prenotazioneId) throws Exception {
        mockMvc.perform(post("/api/prenotazioni/" + prenotazioneId + "/annulla")
                        .with(TestJwt.conRuoliRealm(subject, "VIAGGIATORE")))
                .andExpect(status().isOk());
    }

    private BigDecimal saldo() throws Exception {
        String corpo = mockMvc.perform(get("/api/prenotazioni/saldo/organizzatore")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new BigDecimal(corpo.trim());
    }
}
