package com.unical.travelapp.backend.identity.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Client HTTP dedicato all'Admin REST API di Keycloak.
 *
 * <p>Costruito con {@code RestClient.builder()} e non con il builder auto-configurato di
 * Spring Boot: quello erediterebbe l'{@code ObjectMapper} dell'applicazione, che ha
 * {@code fail-on-unknown-properties=true}. Va benissimo per i payload in ingresso dai
 * client, ma qui significherebbe rompersi al primo campo nuovo introdotto da una versione
 * di Keycloak. Le risposte vengono comunque lette come mappe, quindi il vincolo non si
 * applica; l'isolamento resta un'ulteriore garanzia.
 *
 * <p>I timeout sono espliciti e stretti: senza, una Keycloak irraggiungibile terrebbe
 * occupato un thread del server per il timeout di default del sistema operativo, e un
 * endpoint pubblico come la registrazione diventerebbe una leva per esaurire il pool.
 */
@Configuration
public class KeycloakAdminConfig {

    @Bean
    public RestClient keycloakAdminRestClient(
            @Value("${app.keycloak.admin.base-url}") String baseUrl,
            @Value("${app.keycloak.admin.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${app.keycloak.admin.read-timeout-ms:10000}") long readTimeoutMs) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                // nessun redirect automatico: un 3xx dall'endpoint admin e' un sintomo di
                // configurazione errata (es. base-url sbagliata), non qualcosa da seguire
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
