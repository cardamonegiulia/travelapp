package com.unical.travelapp.backend.security.fase1_authz;

import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 1 - deny-by-default.
 *
 * <p>Senza token nessuna rotta /api/** deve rispondere 200: la catena deve restituire 401
 * (autenticazione mancante), non 403 e soprattutto non il contenuto. Qualunque rotta non
 * mappata esplicitamente nella SecurityConfig deve essere negata.
 *
 * <p>Se sparisse {@code .anyRequest().denyAll()} o {@code .requestMatchers("/api/**").authenticated()}
 * questi test diventerebbero rossi.
 */
class DenyByDefaultSecurityTest extends SecurityIntegrationTestBase {

    @ParameterizedTest(name = "{0} {1} senza token -> 401")
    @CsvSource({
            "GET,    /api/utenti",
            "GET,    /api/utenti/1",
            "POST,   /api/utenti",
            "PUT,    /api/utenti/1",
            "DELETE, /api/utenti/1",
            "POST,   /api/utenti/me",
            "GET,    /api/itinerari",
            "GET,    /api/itinerari/1",
            "POST,   /api/itinerari",
            "DELETE, /api/itinerari/1",
            "GET,    /api/attivita",
            "GET,    /api/attivita/1",
            "POST,   /api/attivita/con-sessioni",
            "DELETE, /api/attivita/1",
            "POST,   /api/prenotazioni",
            "GET,    /api/prenotazioni/1",
            "GET,    /api/prenotazioni/utente/1",
            "POST,   /api/prenotazioni/1/paga",
            "POST,   /api/prenotazioni/1/annulla",
            "GET,    /api/preferiti",
            "POST,   /api/preferiti",
            "DELETE, /api/preferiti",
            "GET,    /api/recensioni/1",
            "GET,    /api/recensioni/itinerario/1",
            "GET,    /api/recensioni/itinerario/1/media",
            "POST,   /api/recensioni",
            "DELETE, /api/recensioni/1"
    })
    void ogniRottaApiRichiedeUnTokenERispondeCon401(String metodo, String percorso) throws Exception {
        MvcResult risultato = mockMvc.perform(richiesta(metodo, percorso))
                .andExpect(status().isUnauthorized())
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @ParameterizedTest(name = "rotta non mappata {0} -> negata")
    @ValueSource(strings = {
            "/api/nonesiste",
            "/internal/qualcosa",
            "/admin",
            "/",
            "/env",
            "/metrics",
            "/h2-console"
    })
    void leRotteNonMappateSonoNegateAnchePerGliAnonimi(String percorso) throws Exception {
        MvcResult risultato = mockMvc.perform(richiesta("GET", percorso)).andReturn();

        assertThat(risultato.getResponse().getStatus())
                .as("nessuna rotta non mappata deve rispondere 200 a un anonimo")
                .isIn(401, 403, 404);
        NessunLeak.verifica(risultato);
    }

    @Test
    void ilPathTraversalNellaUrlVieneRifiutatoPrimaDiArrivareAlControllore() throws Exception {
        // StrictHttpFirewall di Spring Security blocca i segmenti "../" con 400,
        // senza normalizzare la URL e senza raggiungere alcun handler
        mockMvc.perform(richiesta("GET", "/api/utenti/../../internal"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaRottaNonMappataFuoriDaApiEnegataAncheConTokenValido() throws Exception {
        // .anyRequest().denyAll(): nemmeno un ADMIN autenticato passa su una rotta non prevista
        mockMvc.perform(richiesta("GET", "/internal/qualcosa")
                        .with(TestJwt.conRuoliRealm(SUB_ADMIN, "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unTokenMalformatoNonAutentica() throws Exception {
        MvcResult risultato = mockMvc.perform(richiesta("GET", "/api/itinerari")
                        .header("Authorization", "Bearer non-e-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        NessunLeak.verifica(risultato);
    }

    @Test
    void loSchemaDiAutorizzazioneNonBearerNonAutentica() throws Exception {
        mockMvc.perform(richiesta("GET", "/api/itinerari")
                        .header("Authorization", "Basic YWRtaW46YWRtaW4="))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder richiesta(String metodo, String percorso) {
        return request(HttpMethod.valueOf(metodo.trim()), percorso.trim());
    }
}
