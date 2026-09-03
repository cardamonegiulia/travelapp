package com.unical.travelapp.backend.security.fase3_risorse;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestPropertySource(properties = {
        "app.ratelimit.authenticated.capacity=5",
        "app.ratelimit.anonymous.capacity=3"
})
class RateLimitSecurityTest extends SecurityIntegrationTestBase {

    private static final int CAPIENZA_AUTENTICATA = 5;

    @BeforeEach
    void utenti() {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
    }

    private MvcResult chiamata(String subject) throws Exception {
        return mockMvc.perform(get("/api/itinerari")
                .with(TestJwt.conRuoliRealm(subject, "VIAGGIATORE"))).andReturn();
    }

    @Test
    void oltreLaCapienzaLaRichiestaSuccessivaRiceve429() throws Exception {
        String subject = "sub-quota-esaurita";
        utente(subject, Ruolo.VIAGGIATORE);

        for (int i = 1; i <= CAPIENZA_AUTENTICATA; i++) {
            assertThat(chiamata(subject).getResponse().getStatus())
                    .as("la richiesta %d entro la capienza deve passare", i)
                    .isEqualTo(200);
        }

        MvcResult oltre = chiamata(subject);

        assertThat(oltre.getResponse().getStatus())
                .as("la richiesta N+1 deve essere limitata")
                .isEqualTo(429);
        assertThat(oltre.getResponse().getHeader("Retry-After"))
                .as("il 429 deve dire al client quando riprovare")
                .isNotNull()
                .satisfies(valore -> assertThat(Integer.parseInt(valore)).isPositive());
        assertThat(oltre.getResponse().getHeader("X-RateLimit-Limit"))
                .isEqualTo(String.valueOf(CAPIENZA_AUTENTICATA));
        assertThat(oltre.getResponse().getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        NessunLeak.verifica(oltre);
    }

    @Test
    void gliHeaderDiQuotaSonoCoerentiRichiestaDopoRichiesta() throws Exception {
        String subject = "sub-conteggio";
        utente(subject, Ruolo.VIAGGIATORE);

        for (int i = 1; i <= CAPIENZA_AUTENTICATA; i++) {
            MvcResult risultato = chiamata(subject);
            assertThat(risultato.getResponse().getHeader("X-RateLimit-Limit"))
                    .isEqualTo(String.valueOf(CAPIENZA_AUTENTICATA));
            assertThat(risultato.getResponse().getHeader("X-RateLimit-Remaining"))
                    .as("dopo %d richieste devono restare %d gettoni", i, CAPIENZA_AUTENTICATA - i)
                    .isEqualTo(String.valueOf(CAPIENZA_AUTENTICATA - i));
        }
    }

    @Test
    void iBucketSonoIsolatiPerUtente() throws Exception {
        String vittima = "sub-isolamento-a";
        String altro = "sub-isolamento-b";
        utente(vittima, Ruolo.VIAGGIATORE);
        utente(altro, Ruolo.VIAGGIATORE);

        for (int i = 0; i < CAPIENZA_AUTENTICATA + 2; i++) {
            chiamata(vittima);
        }
        assertThat(chiamata(vittima).getResponse().getStatus()).isEqualTo(429);

        assertThat(chiamata(altro).getResponse().getStatus())
                .as("l'utente che ha esaurito la quota non deve bloccare gli altri")
                .isEqualTo(200);
    }

    @Test
    void laChiaveDelBucketEIlSubDelTokenNonLoUsername() throws Exception {
        String subject = "sub-chiave-stabile";
        utente(subject, Ruolo.VIAGGIATORE);

        for (int i = 0; i < CAPIENZA_AUTENTICATA; i++) {
            mockMvc.perform(get("/api/itinerari")
                    .with(TestJwt.conUsernameDiverso(subject, "alias-" + i, "VIAGGIATORE"))).andReturn();
        }

        MvcResult oltre = mockMvc.perform(get("/api/itinerari")
                .with(TestJwt.conUsernameDiverso(subject, "alias-finale", "VIAGGIATORE"))).andReturn();

        assertThat(oltre.getResponse().getStatus())
                .as("cambiare username non deve azzerare la quota")
                .isEqualTo(429);
    }

    @Test
    void ilRateLimitAnonimoConta() throws Exception {
        int limitati = 0;
        for (int i = 0; i < 12; i++) {
            MvcResult risultato = mockMvc.perform(get("/api/itinerari")).andReturn();
            if (risultato.getResponse().getStatus() == 429) {
                limitati++;
            }
        }

        assertThat(limitati)
                .as("superata la capienza anonima le richieste devono essere limitate")
                .isPositive();
    }

    @Test
    void ilRateLimitAnonimoDistingueGliIndirizziIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/itinerari").with(richiesta -> {
                richiesta.setRemoteAddr("10.0.0.1");
                return richiesta;
            })).andReturn();
        }

        MvcResult altroIp = mockMvc.perform(get("/api/itinerari").with(richiesta -> {
            richiesta.setRemoteAddr("10.0.0.2");
            return richiesta;
        })).andReturn();

        assertThat(altroIp.getResponse().getStatus())
                .as("un IP che ha esaurito la quota non deve bloccare gli altri IP")
                .isNotEqualTo(429);
    }

    @Test
    void xForwardedForNonPermetteDiAggirareIlLimiteAnonimo() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/itinerari").with(richiesta -> {
                richiesta.setRemoteAddr("10.1.1.1");
                return richiesta;
            })).andReturn();
        }

        MvcResult conHeaderFalsificato = mockMvc.perform(get("/api/itinerari")
                .header("X-Forwarded-For", "203.0.113." + System.nanoTime() % 250)
                .with(richiesta -> {
                    richiesta.setRemoteAddr("10.1.1.1");
                    return richiesta;
                })).andReturn();

        assertThat(conHeaderFalsificato.getResponse().getStatus())
                .as("falsificare X-Forwarded-For non deve azzerare la quota dell'IP reale")
                .isEqualTo(429);
    }

    @Test
    void il429PortaConSeGliHeaderDiSicurezza() throws Exception {
        String subject = "sub-header-429";
        utente(subject, Ruolo.VIAGGIATORE);
        for (int i = 0; i < CAPIENZA_AUTENTICATA + 1; i++) {
            chiamata(subject);
        }

        MvcResult limitata = chiamata(subject);
        assertThat(limitata.getResponse().getStatus()).isEqualTo(429);
        assertThat(limitata.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(limitata.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(limitata.getResponse().getHeader("Content-Security-Policy")).isNotNull();
    }

    @Test
    void ilCorpoDel429NonEspoDettagliInterni() throws Exception {
        String subject = "sub-corpo-429";
        utente(subject, Ruolo.VIAGGIATORE);
        for (int i = 0; i < CAPIENZA_AUTENTICATA + 1; i++) {
            chiamata(subject);
        }

        MvcResult limitata = chiamata(subject);
        assertThat(limitata.getResponse().getStatus()).isEqualTo(429);
        NessunLeak.verifica(limitata);

        assertThat(limitata.getResponse().getContentType()).contains("application/json");
        assertThat(objectMapper.readTree(limitata.getResponse().getContentAsString()).get("status").asInt())
                .isEqualTo(429);
    }
}
