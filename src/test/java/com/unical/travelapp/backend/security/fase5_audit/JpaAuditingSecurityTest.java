package com.unical.travelapp.backend.security.fase5_audit;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 5 - JPA Auditing: chi ha creato o modificato una riga.
 *
 * <p>Il valore di {@code creatoDa} deve venire dal claim {@code sub} del token, mai dal
 * client: se fosse influenzabile dall'esterno l'audit trail sarebbe falsificabile e quindi
 * inutile come prova.
 */
class JpaAuditingSecurityTest extends SecurityIntegrationTestBase {

    /**
     * Da quando l'aggiornamento del profilo viene propagato all'IdP, una PUT su
     * {@code /api/utenti} passa anche da qui. Non e' l'oggetto di questi test: si sostituisce
     * con un mock perche' il profilo "test" punta di proposito a un Keycloak irraggiungibile.
     */
    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    private Utente organizzatore;

    @BeforeEach
    void utenti() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
    }

    private String payloadItinerario(String titolo, String extra) {
        return "{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"" + titolo + "\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                + "\"durataGiorni\":1,\"maxPartecipanti\":2" + extra + "}";
    }

    @Test
    void allaCreazioneCreatoDaEIlSubDelTokenECreatoIlEValorizzato() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadItinerario("Con audit", "")))
                .andExpect(status().isOk());

        Itinerario salvato = itinerarioRepository.findAll().stream()
                .filter(i -> "Con audit".equals(i.getTitolo()))
                .findFirst().orElseThrow();

        assertThat(salvato.getCreatoDa())
                .as("l'autore e' il subject del token")
                .isEqualTo(SUB_ORGANIZZATORE);
        assertThat(salvato.getCreatoIl())
                .as("la data di creazione deve essere valorizzata dal server")
                .isNotNull()
                .isBefore(LocalDateTime.now().plusMinutes(1));
    }

    @Test
    void unCreatoDaInviatoDalClientVieneRifiutato() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadItinerario("Falsificato",
                                ",\"creatoDa\":\"vittima\",\"creatoIl\":\"2000-01-01T00:00:00\"")))
                .andExpect(status().isBadRequest());

        assertThat(itinerarioRepository.findAll())
                .as("il payload con campi di audit non deve creare nulla")
                .noneMatch(i -> "Falsificato".equals(i.getTitolo()));
    }

    @Test
    void suUnAggiornamentoCambiaSoloIlModificatoDa() throws Exception {
        Utente utenteA = utenteRepository.findByKeycloakId(SUB_UTENTE_A).orElseThrow();
        String creatoDaIniziale = utenteA.getCreatoDa();
        LocalDateTime creatoIlIniziale = utenteA.getCreatoIl();

        mockMvc.perform(put("/api/utenti/" + utenteA.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"NomeAggiornato\"}"))
                .andExpect(status().isOk());

        Utente dopo = utenteRepository.findById(utenteA.getId()).orElseThrow();

        assertThat(dopo.getModificatoDa())
                .as("chi ha modificato e' il subject del token")
                .isEqualTo(SUB_UTENTE_A);
        assertThat(dopo.getModificatoIl()).isNotNull();
        assertThat(dopo.getCreatoDa())
                .as("i campi di creazione non devono cambiare in aggiornamento")
                .isEqualTo(creatoDaIniziale);
        assertThat(dopo.getCreatoIl()).isEqualTo(creatoIlIniziale);
    }

    @Test
    void utentiDiversiLascianoTracceDiverse() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadItinerario("Primo", ""))).andExpect(status().isOk());

        utente("sub-organizzatore-due", Ruolo.ORGANIZZATORE);
        mockMvc.perform(post("/api/itinerari")
                .with(TestJwt.conRuoliRealm("sub-organizzatore-due", "ORGANIZZATORE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadItinerario("Secondo", ""))).andExpect(status().isOk());

        assertThat(itinerarioRepository.findAll())
                .filteredOn(i -> "Primo".equals(i.getTitolo()))
                .singleElement()
                .satisfies(i -> assertThat(i.getCreatoDa()).isEqualTo(SUB_ORGANIZZATORE));
        assertThat(itinerarioRepository.findAll())
                .filteredOn(i -> "Secondo".equals(i.getTitolo()))
                .singleElement()
                .satisfies(i -> assertThat(i.getCreatoDa()).isEqualTo("sub-organizzatore-due"));
    }

    @Test
    void lIdentitaDiAuditNonDipendeDallUsernameDelToken() throws Exception {
        mockMvc.perform(post("/api/itinerari")
                        .with(TestJwt.conUsernameDiverso(SUB_ORGANIZZATORE, "alias-arbitrario", "ORGANIZZATORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadItinerario("Con alias", "")))
                .andExpect(status().isOk());

        assertThat(itinerarioRepository.findAll())
                .filteredOn(i -> "Con alias".equals(i.getTitolo()))
                .singleElement()
                .satisfies(i -> assertThat(i.getCreatoDa())
                        .as("conta il sub, non il preferred_username")
                        .isEqualTo(SUB_ORGANIZZATORE));
    }

    @Test
    void leEntitaDiDominioEreditanoTutteITracciamentoDiAudit() {
        // se una nuova entita' dimenticasse di estendere Auditable non avrebbe traccia di
        // chi l'ha creata: qui si verifica che le entita' esistenti la abbiano
        assertThat(com.unical.travelapp.backend.common.audit.Auditable.class)
                .isAssignableFrom(com.unical.travelapp.backend.identity.entity.Utente.class)
                .isAssignableFrom(com.unical.travelapp.backend.booking.entity.Prenotazione.class)
                .isAssignableFrom(com.unical.travelapp.backend.booking.entity.Pagamento.class)
                .isAssignableFrom(com.unical.travelapp.backend.catalog.entity.Itinerario.class)
                .isAssignableFrom(com.unical.travelapp.backend.catalog.entity.SingolaAttivita.class)
                .isAssignableFrom(com.unical.travelapp.backend.experience.models.Recensione.class)
                .isAssignableFrom(com.unical.travelapp.backend.experience.models.ListaPreferiti.class);
    }
}
