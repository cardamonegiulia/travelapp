package com.unical.travelapp.backend.identity;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Riallineamento del campo {@code utenti.ruolo} al ruolo realm del token, su
 * {@code POST /api/utenti/me}.
 *
 * <p>Il ruolo che conta per l'autorizzazione e' sempre quello del token; la colonna locale
 * e' una copia per il frontend. Questi test coprono il momento in cui le due divergono:
 * un amministratore cambia il ruolo su Keycloak e l'utente rientra nell'app.
 */
class SincronizzazioneRuoloTest extends SecurityIntegrationTestBase {

    @Test
    void unaPromozioneFattaSuKeycloakArrivaAlRecordLocale() throws Exception {
        Utente esistente = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);

        // stesso subject, ma ora il token porta ORGANIZZATORE: e' la promozione fatta in console
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "ORGANIZZATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("ORGANIZZATORE"));

        assertThat(utenteRepository.findById(esistente.getId()).orElseThrow().getRuolo())
                .as("il ruolo locale deve seguire quello del token, non restare al valore della creazione")
                .isEqualTo(Ruolo.ORGANIZZATORE);
    }

    @Test
    void unTokenSenzaRuoliApplicativiNonDeclassaLUtente() throws Exception {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);

        // nessun ruolo nel token: e' il sintomo di un client scope configurato male,
        // non la prova che l'utente sia stato degradato a VIAGGIATORE
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.senzaRuoli(SUB_ORGANIZZATORE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("ORGANIZZATORE"));

        assertThat(utenteRepository.findById(organizzatore.getId()).orElseThrow().getRuolo())
                .as("un token senza ruoli non deve sovrascrivere un ruolo gia' noto")
                .isEqualTo(Ruolo.ORGANIZZATORE);
    }

    @Test
    void ancheUnDeclassamentoEsplicitoViaggiaVersoIlRecordLocale() throws Exception {
        Utente esistente = utente(SUB_UTENTE_B, Ruolo.ORGANIZZATORE);

        // qui il ruolo c'e' ed e' VIAGGIATORE: e' una decisione dell'amministratore, va seguita
        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("VIAGGIATORE"));

        assertThat(utenteRepository.findById(esistente.getId()).orElseThrow().getRuolo())
                .isEqualTo(Ruolo.VIAGGIATORE);
    }

    @Test
    void conPiuRuoliNelTokenVinceIlPiuAlto() throws Exception {
        Utente esistente = utente(SUB_ADMIN, Ruolo.VIAGGIATORE);

        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "VIAGGIATORE", "ORGANIZZATORE", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("ADMIN"));

        assertThat(utenteRepository.findById(esistente.getId()).orElseThrow().getRuolo())
                .isEqualTo(Ruolo.ADMIN);
    }

    @Test
    void ilRuoloGiaAllineatoRestaInvariato() throws Exception {
        Utente esistente = utente(SUB_UTENTE_A, Ruolo.ORGANIZZATORE);

        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "ORGANIZZATORE")))
                .andExpect(status().isOk());

        Utente dopo = utenteRepository.findById(esistente.getId()).orElseThrow();
        assertThat(dopo.getRuolo()).isEqualTo(Ruolo.ORGANIZZATORE);
        assertThat(dopo.getEmail())
                .as("il riallineamento non deve toccare gli altri campi del profilo")
                .isEqualTo(esistente.getEmail());
    }
}
