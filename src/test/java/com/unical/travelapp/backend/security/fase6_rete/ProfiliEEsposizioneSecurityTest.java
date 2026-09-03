package com.unical.travelapp.backend.security.fase6_rete;

import com.unical.travelapp.backend.TravelappBackendApplication;
import com.unical.travelapp.backend.config.ProdSecurityChecksConfig;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ProfiliEEsposizioneSecurityTest extends SecurityIntegrationTestBase {

    @BeforeEach
    void utenti() {
        utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);
    }

    @Test
    void inProduzioneUnIssuerNonHttpsFaFallireLAvvio() {
        ProdSecurityChecksConfig config = new ProdSecurityChecksConfig();
        ReflectionTestUtils.setField(config, "issuerUri", "http://travelapp-keycloak:8080/realms/travelapp");

        assertThatThrownBy(config::verificaIssuerHttps)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void inProduzioneUnIssuerNulloFaFallireLAvvio() {
        ProdSecurityChecksConfig config = new ProdSecurityChecksConfig();
        ReflectionTestUtils.setField(config, "issuerUri", null);

        assertThatThrownBy(config::verificaIssuerHttps).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void inProduzioneUnIssuerHttpsEAccettato() {
        ProdSecurityChecksConfig config = new ProdSecurityChecksConfig();
        ReflectionTestUtils.setField(config, "issuerUri", "https://auth.travelapp.example/realms/travelapp");

        assertThatCode(config::verificaIssuerHttps).doesNotThrowAnyException();
    }

    @Test
    void ilContestoConProfiloProdNonPartteConUnIssuerHttp() {
        assertThatThrownBy(() -> avviaContesto("prod",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8090/realms/travelapp"))
                .hasStackTraceContaining("HTTPS");
    }

    @Test
    void inProduzioneSwaggerEOpenApiSonoDisattivati() {
        try (ConfigurableApplicationContext contesto = avviaContesto("prod",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example/realms/travelapp",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example/certs")) {

            assertThat(contesto.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class))
                    .as("swagger-ui non deve essere esposto in produzione")
                    .isFalse();
            assertThat(contesto.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class))
                    .as("la specifica OpenAPI non deve essere esposta in produzione")
                    .isFalse();
            assertThat(contesto.getEnvironment().getProperty("app.security.require-https", Boolean.class))
                    .as("in produzione l'HTTPS e' obbligatorio")
                    .isTrue();
            assertThat(contesto.getEnvironment().getProperty("server.error.include-stacktrace"))
                    .isEqualTo("never");
            assertThat(contesto.getEnvironment().getProperty("server.error.include-message"))
                    .isEqualTo("never");
        }
    }

    @Test
    void ilProfiloDevRilassaSoloIlMessaggioDiErroreENonEspoSegreti() {
        try (ConfigurableApplicationContext contesto = avviaContesto("dev")) {
            assertThat(contesto.getEnvironment().getProperty("server.error.include-message"))
                    .as("in sviluppo il messaggio aiuta il debug")
                    .isEqualTo("always");
            assertThat(contesto.getEnvironment().getProperty("server.error.include-stacktrace"))
                    .as("ma lo stack trace resta escluso anche in sviluppo")
                    .isEqualTo("never");
        }
    }

    @Test
    void gliEndpointActuatorRiservatiSonoNegatiAiNonAdmin() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/actuator/env")
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getResponse().getStatus())
                .as("un viaggiatore non deve leggere la configurazione del server")
                .isEqualTo(403);
    }

    @Test
    void gliEndpointActuatorRiservatiSonoNegatiAgliAnonimi() throws Exception {
        for (String percorso : new String[]{"/actuator/env", "/actuator/beans", "/actuator/configprops",
                "/actuator/loggers", "/actuator/heapdump", "/actuator/threaddump"}) {
            MvcResult risultato = mockMvc.perform(get(percorso)).andReturn();
            assertThat(risultato.getResponse().getStatus())
                    .as("%s non deve essere accessibile in anonimo", percorso)
                    .isIn(401, 403);
        }
    }

    @Test
    void healthEInfoSonoPubbliciPerDefinizioneMaNonEspongonoDati() throws Exception {
        for (String percorso : new String[]{"/actuator/health", "/actuator/info"}) {
            MvcResult risultato = mockMvc.perform(get(percorso)).andReturn();
            assertThat(risultato.getResponse().getStatus())
                    .as("%s e' permitAll ma l'endpoint non esiste: 404, mai 200 con dati", percorso)
                    .isEqualTo(404);
        }
    }

    @Test
    void laSpecificaOpenApiNonRivelaSegretiQuandoEEsposta() throws Exception {
        MvcResult risultato = mockMvc.perform(get("/v3/api-docs")).andReturn();

        if (risultato.getResponse().getStatus() == 200) {
            String corpo = risultato.getResponse().getContentAsString().toLowerCase();
            assertThat(corpo)
                    .doesNotContain("\"password\":\"")
                    .doesNotContain("\"password\": \"")
                    .doesNotContain("client_secret")
                    .doesNotContain("clientsecret")
                    .doesNotContain("db_password");
        }
    }

    private ConfigurableApplicationContext avviaContesto(String profilo, String... proprieta) {
        String[] argomenti = new String[proprieta.length + 3];
        argomenti[0] = "--spring.profiles.active=" + profilo;
        argomenti[1] = "--spring.datasource.url=jdbc:h2:mem:profilo-" + profilo
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        argomenti[2] = "--spring.datasource.username=sa";
        for (int i = 0; i < proprieta.length; i++) {
            argomenti[i + 3] = "--" + proprieta[i];
        }

        return new SpringApplicationBuilder(TravelappBackendApplication.class)
                .web(org.springframework.boot.WebApplicationType.NONE)
                .run(argomenti);
    }
}
