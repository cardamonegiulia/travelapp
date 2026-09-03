package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

class MatriceRuoliSecurityTest extends SecurityIntegrationTestBase {

    private static final String QUALSIASI_AUTENTICATO = "*";

    private Long idItinerario;
    private Long idUtenteA;

    @BeforeEach
    void datiDiBase() {
        Utente organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        Utente utenteA = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
        Itinerario itinerario = itinerario(organizzatore);
        this.idItinerario = itinerario.getId();
        this.idUtenteA = utenteA.getId();
    }

    static Stream<Arguments> matrice() {
        return Stream.of(
                Arguments.of("GET", "/api/utenti", "ADMIN", null),
                Arguments.of("POST", "/api/utenti", "ADMIN",
                        "{\"keycloakId\":\"x\",\"nome\":\"Ada\",\"cognome\":\"Lovelace\",\"email\":\"ada@example.test\"}"),
                Arguments.of("GET", "/api/itinerari", QUALSIASI_AUTENTICATO, null),
                Arguments.of("GET", "/api/itinerari/{itinerario}", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/itinerari", "ORGANIZZATORE,ADMIN",
                        "{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"durataGiorni\":2,\"maxPartecipanti\":5}"),
                Arguments.of("DELETE", "/api/itinerari/{itinerario}", "ORGANIZZATORE,ADMIN", null),
                Arguments.of("GET", "/api/attivita", QUALSIASI_AUTENTICATO, null),
                Arguments.of("GET", "/api/attivita/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("DELETE", "/api/attivita/1", "ORGANIZZATORE,ADMIN", null),
                Arguments.of("POST", "/api/prenotazioni", QUALSIASI_AUTENTICATO,
                        "{\"disponibilitaItinerarioId\":1,\"numeroPartecipanti\":1}"),
                Arguments.of("GET", "/api/prenotazioni/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/prenotazioni/1/paga", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/prenotazioni/1/annulla", QUALSIASI_AUTENTICATO, null),
                Arguments.of("GET", "/api/preferiti", QUALSIASI_AUTENTICATO, null),
                Arguments.of("GET", "/api/preferiti/condivise-con-me", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/preferiti", QUALSIASI_AUTENTICATO, "{\"nome\":\"Lista di prova\"}"),
                Arguments.of("GET", "/api/preferiti/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("PUT", "/api/preferiti/1", QUALSIASI_AUTENTICATO, "{\"nome\":\"Rinominata\"}"),
                Arguments.of("DELETE", "/api/preferiti/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/preferiti/1/itinerari", QUALSIASI_AUTENTICATO, "{\"itinerarioId\":1}"),
                Arguments.of("DELETE", "/api/preferiti/1/itinerari/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/preferiti/itinerari", QUALSIASI_AUTENTICATO, "{\"itinerarioId\":1}"),
                Arguments.of("DELETE", "/api/preferiti/itinerari/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/preferiti/1/condivisioni", QUALSIASI_AUTENTICATO, "{\"utenteId\":1}"),
                Arguments.of("DELETE", "/api/preferiti/1/condivisioni/2", QUALSIASI_AUTENTICATO, null),
                Arguments.of("GET", "/api/recensioni/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("GET", "/api/recensioni/itinerario/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("GET", "/api/recensioni/itinerario/1/media", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/recensioni", QUALSIASI_AUTENTICATO,
                        "{\"itinerarioId\":1,\"votazione\":4,\"comm\":\"ok\"}"),
                Arguments.of("DELETE", "/api/recensioni/1", QUALSIASI_AUTENTICATO, null),
                Arguments.of("POST", "/api/utenti/me", QUALSIASI_AUTENTICATO, null)
        );
    }

    @ParameterizedTest(name = "{0} {1} - anonimo -> 401")
    @MethodSource("matrice")
    void anonimoSempreRespintoCon401(String metodo, String percorso, String ruoliAmmessi, String payload)
            throws Exception {
        MvcResult risultato = mockMvc.perform(costruisci(metodo, percorso, payload))
                .andReturn();

        assertThat(risultato.getResponse().getStatus())
                .as("%s %s da anonimo deve essere 401", metodo, percorso)
                .isEqualTo(401);
        NessunLeak.verifica(risultato);
    }

    @ParameterizedTest(name = "{0} {1} - ruoli ammessi: {2}")
    @MethodSource("matrice")
    void ogniRuoloRispettaLaMatrice(String metodo, String percorso, String ruoliAmmessi, String payload)
            throws Exception {
        for (String ruolo : List.of("VIAGGIATORE", "ORGANIZZATORE", "ADMIN")) {
            String subject = switch (ruolo) {
                case "ADMIN" -> SUB_ADMIN;
                case "ORGANIZZATORE" -> SUB_ORGANIZZATORE;
                default -> SUB_UTENTE_A;
            };

            MvcResult risultato = mockMvc.perform(costruisci(metodo, percorso, payload)
                            .with(TestJwt.conRuoliRealm(subject, ruolo)))
                    .andReturn();
            int status = risultato.getResponse().getStatus();

            boolean ammesso = QUALSIASI_AUTENTICATO.equals(ruoliAmmessi)
                    || Set.of(ruoliAmmessi.split(",")).contains(ruolo);

            if (ammesso) {
                assertThat(status)
                        .as("%s %s con ruolo %s non deve essere respinto dall'autorizzazione", metodo, percorso, ruolo)
                        .isNotIn(401, 403);
            } else {
                assertThat(status)
                        .as("%s %s con ruolo %s deve essere 403", metodo, percorso, ruolo)
                        .isEqualTo(403);
                NessunLeak.verifica(risultato);
            }
        }
    }

    @Test
    void unUtenteAutenticatoSenzaRuoliApplicativiNonAccedeAlleRotteRiservate() throws Exception {
        List<MvcResult> risultati = new ArrayList<>();
        risultati.add(mockMvc.perform(costruisci("GET", "/api/utenti", null)
                .with(TestJwt.senzaRuoli(SUB_UTENTE_A))).andReturn());
        risultati.add(mockMvc.perform(costruisci("POST", "/api/itinerari",
                        "{\"programma\":[{\"titolo\":\"Giornata 1\",\"descrizione\":\"Attivita della giornata\"}],\"titolo\":\"T\",\"destinazionePrincipale\":\"D\",\"prezzoBase\":10.0,"
                                + "\"durataGiorni\":2,\"maxPartecipanti\":5}")
                .with(TestJwt.senzaRuoli(SUB_UTENTE_A))).andReturn());

        for (MvcResult risultato : risultati) {
            assertThat(risultato.getResponse().getStatus()).isEqualTo(403);
            NessunLeak.verifica(risultato);
        }
    }

    @Test
    void iRuoliClientDiKeycloakSonoEquivalentiAiRuoliRealm() throws Exception {
        mockMvc.perform(costruisci("GET", "/api/utenti", null)
                        .with(TestJwt.conRuoliClient(SUB_ADMIN, "ADMIN")))
                .andReturn();

        int conRuoloClient = mockMvc.perform(costruisci("GET", "/api/utenti", null)
                .with(TestJwt.conRuoliClient(SUB_ADMIN, "ADMIN"))).andReturn().getResponse().getStatus();
        int conRuoloRealm = mockMvc.perform(costruisci("GET", "/api/utenti", null)
                .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN"))).andReturn().getResponse().getStatus();

        assertThat(conRuoloClient).isEqualTo(conRuoloRealm).isEqualTo(200);
    }

    @Test
    void unRuoloDiUnAltroClientNonConcedeAlcunPrivilegio() throws Exception {
        mockMvc.perform(costruisci("GET", "/api/utenti", null)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(builder -> builder
                                        .subject(SUB_UTENTE_A)
                                        .claim("resource_access",
                                                java.util.Map.of("un-altro-client",
                                                        java.util.Map.of("roles", List.of("ADMIN")))))
                                .authorities(new com.unical.travelapp.backend.config
                                        .KeycloakRoleConverter(TestJwt.CLIENT_ID))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isForbidden());
    }

    private MockHttpServletRequestBuilder costruisci(String metodo, String percorso, String payload) {
        String risolto = percorso
                .replace("{itinerario}", String.valueOf(idItinerario))
                .replace("{utenteA}", String.valueOf(idUtenteA));
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(metodo), risolto);
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        return builder;
    }
}
