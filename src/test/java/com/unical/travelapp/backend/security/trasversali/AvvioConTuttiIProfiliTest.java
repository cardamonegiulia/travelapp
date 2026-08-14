package com.unical.travelapp.backend.security.trasversali;

import com.unical.travelapp.backend.TravelappBackendApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Il contesto deve avviarsi con ciascun profilo previsto.
 *
 * <p>Una configurazione di sicurezza che rompe l'avvio di un profilo verrebbe rimossa in
 * fretta; una che si avvia ma disattiva silenziosamente un controllo e' peggio. Qui si
 * verifica l'avvio e, per ogni profilo, che i controlli chiave restino accesi.
 */
class AvvioConTuttiIProfiliTest {

    private ConfigurableApplicationContext avvia(String... profili) {
        String elencoProfili = String.join(",", profili);
        return new SpringApplicationBuilder(TravelappBackendApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.profiles.active=" + elencoProfili,
                        "--spring.datasource.url=jdbc:h2:mem:avvio-" + elencoProfili.replace(',', '-')
                                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=create-drop",
                        "--spring.security.oauth2.resourceserver.jwt.issuer-uri="
                                + (elencoProfili.contains("prod")
                                ? "https://auth.example/realms/travelapp"
                                : "http://localhost:8090/realms/travelapp"),
                        "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri="
                                + (elencoProfili.contains("prod")
                                ? "https://auth.example/realms/travelapp/protocol/openid-connect/certs"
                                : "http://localhost:8090/realms/travelapp/protocol/openid-connect/certs"));
    }

    @ParameterizedTest(name = "il contesto parte con il profilo {0}")
    @ValueSource(strings = {"default", "dev", "test", "prod"})
    void ilContestoSiAvviaConOgniProfilo(String profilo) {
        try (ConfigurableApplicationContext contesto = avvia(profilo)) {
            assertThat(contesto.isActive()).isTrue();

            assertThat(contesto.getEnvironment().getProperty("app.security.expected-audience"))
                    .as("l'audience attesa deve essere configurata in ogni profilo")
                    .isNotBlank();
            assertThat(contesto.getEnvironment()
                    .getProperty("spring.jackson.deserialization.fail-on-unknown-properties", Boolean.class))
                    .as("il rifiuto dei campi sconosciuti non deve dipendere dal profilo")
                    .isTrue();
            assertThat(contesto.getEnvironment().getProperty("server.error.include-stacktrace"))
                    .as("nessun profilo deve esporre lo stack trace")
                    .isEqualTo("never");
        }
    }

    @Test
    void iBeanDiSicurezzaEsistonoInOgniProfilo() {
        for (String profilo : new String[]{"default", "dev", "prod"}) {
            try (ConfigurableApplicationContext contesto = avvia(profilo)) {
                assertThat(contesto.getBean(org.springframework.security.oauth2.jwt.JwtDecoder.class))
                        .as("profilo %s: il JwtDecoder deve esistere", profilo)
                        .isNotNull();
                assertThat(contesto.getBean(com.unical.travelapp.backend.config.RateLimitFilter.class))
                        .as("profilo %s: il filtro di rate limit deve esistere", profilo)
                        .isNotNull();
                assertThat(contesto.getBean(com.unical.travelapp.backend.common.audit.AuditLogger.class))
                        .isNotNull();
                assertThat(contesto.getBean(com.unical.travelapp.backend.common.audit.SecurityAuditorAware.class))
                        .isNotNull();
                assertThat(contesto.getBean(com.unical.travelapp.backend.exception.GlobalExceptionHandler.class))
                        .isNotNull();
            }
        }
    }

    @Test
    void ilProfiloProdAttivaIControlliAggiuntivi() {
        try (ConfigurableApplicationContext contesto = avvia("prod")) {
            assertThat(contesto.getBean(com.unical.travelapp.backend.config.ProdSecurityChecksConfig.class))
                    .as("in prod i controlli fail-fast devono essere attivi")
                    .isNotNull();
            assertThat(contesto.getEnvironment().getProperty("app.security.require-https", Boolean.class))
                    .isTrue();
        }
    }

    @Test
    void fuoriDaProdIControlliDiProduzioneNonSonoAttivi() {
        try (ConfigurableApplicationContext contesto = avvia("dev")) {
            assertThat(contesto.getBeanNamesForType(
                    com.unical.travelapp.backend.config.ProdSecurityChecksConfig.class))
                    .as("il controllo HTTPS e' specifico del profilo prod")
                    .isEmpty();
        }
    }
}
