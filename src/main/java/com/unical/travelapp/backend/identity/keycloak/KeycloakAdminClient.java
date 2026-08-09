package com.unical.travelapp.backend.identity.keycloak;

import com.unical.travelapp.backend.identity.exception.RegistrazioneNonDisponibileException;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gateway verso l'Admin REST API di Keycloak: unica classe che parla con l'IdP in scrittura.
 *
 * <p>Usa l'API REST tramite {@link RestClient} invece della libreria {@code keycloak-admin-client}:
 * quest'ultima trascinerebbe RESTEasy e l'intero stack JAX-RS dentro un'applicazione Spring
 * che non ne ha altrimenti bisogno, per quattro chiamate HTTP. Le motivazioni estese sono in
 * {@code docs/registrazione-implementazione.md}.
 *
 * <p>Regole rispettate qui: il client secret non compare mai nei log, i corpi di risposta di
 * Keycloak non vengono mai rilanciati verso il chiamante (potrebbero contenere dettagli sulla
 * configurazione del realm) e ogni errore infrastrutturale diventa
 * {@link RegistrazioneNonDisponibileException}, mai un 500 generico senza spiegazione nei log.
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient restClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAdminClient(
            @Qualifier("keycloakAdminRestClient") RestClient restClient,
            @Value("${app.keycloak.admin.realm}") String realm,
            @Value("${app.keycloak.admin.client-id}") String clientId,
            @Value("${app.keycloak.admin.client-secret}") String clientSecret) {
        this.restClient = restClient;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /** Ruolo realm risolto su Keycloak: per assegnarlo servono sia l'id sia il nome. */
    public record RuoloRealm(String id, String nome) {
    }

    /** Dati dell'utente da creare su Keycloak. La password non viene mai loggata. */
    public record NuovoUtenteKeycloak(String username, String email, String nome, String cognome, String password) {
    }

    /**
     * Access token del service account (grant {@code client_credentials}).
     *
     * <p>Deliberatamente non messo in cache: la registrazione e' un'operazione rara, e un
     * token in cache introdurrebbe il caso "token valido secondo noi ma rifiutato da Keycloak"
     * dopo un riavvio del realm, con una gestione del refresh da mantenere.
     */
    public String ottieniTokenAmministrativo() {
        if (!StringUtils.hasText(clientSecret)) {
            log.error("Registrazione non disponibile: 'app.keycloak.admin.client-secret' non è configurato "
                    + "(variabile d'ambiente KEYCLOAK_ADMIN_CLIENT_SECRET)");
            throw new RegistrazioneNonDisponibileException("client secret del service account non configurato");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<?, ?> risposta = esegui(
                () -> restClient.post()
                        .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(Map.class),
                stato -> {
                    log.error("Autenticazione del service account '{}' fallita (HTTP {}): verificare client-id, "
                                    + "client secret e che 'Service accounts roles' sia abilitato sul client",
                            clientId, stato);
                    return new RegistrazioneNonDisponibileException("service account Keycloak non autenticato");
                });

        Object accessToken = risposta == null ? null : risposta.get("access_token");
        if (accessToken == null) {
            log.error("Risposta del token endpoint senza campo access_token per il client '{}'", clientId);
            throw new RegistrazioneNonDisponibileException("risposta del token endpoint priva di access_token");
        }
        return accessToken.toString();
    }

    /**
     * Cerca un ruolo realm per nome. {@link Optional#empty()} se il ruolo non esiste:
     * e' una condizione prevista che il chiamante deve decidere come trattare, non un errore
     * di trasporto.
     */
    public Optional<RuoloRealm> trovaRuoloRealm(String token, String nomeRuolo) {
        Map<?, ?> ruolo;
        try {
            ruolo = restClient.get()
                    .uri("/admin/realms/{realm}/roles/{nomeRuolo}", realm, nomeRuolo)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            if (e.getStatusCode().value() == 403) {
                log.error("Il service account '{}' non è autorizzato a leggere i ruoli del realm '{}': "
                        + "assegnargli il ruolo client 'view-realm' di realm-management", clientId, realm);
            } else {
                log.error("Lettura del ruolo realm '{}' fallita (HTTP {})", nomeRuolo, e.getStatusCode().value());
            }
            throw new RegistrazioneNonDisponibileException("lettura del ruolo realm fallita");
        } catch (ResourceAccessException e) {
            throw keycloakIrraggiungibile(e);
        }

        if (ruolo == null || ruolo.get("id") == null) {
            log.error("Ruolo realm '{}' restituito senza id da Keycloak", nomeRuolo);
            throw new RegistrazioneNonDisponibileException("ruolo realm senza id");
        }
        return Optional.of(new RuoloRealm(String.valueOf(ruolo.get("id")), nomeRuolo));
    }

    /**
     * Crea l'utente e ne restituisce l'id Keycloak (il {@code sub} dei token futuri).
     *
     * @throws UtenteGiaEsistenteException se username o email risultano gia' presenti nel realm
     */
    public String creaUtente(String token, NuovoUtenteKeycloak nuovo) {
        Map<String, Object> credenziale = Map.of(
                "type", "password",
                "value", nuovo.password(),
                // non temporanea: con una password temporanea Keycloak imporrebbe UPDATE_PASSWORD
                // al primo login e il token non verrebbe rilasciato
                "temporary", false);

        Map<String, Object> utente = Map.of(
                "username", nuovo.username(),
                "email", nuovo.email(),
                "firstName", nuovo.nome(),
                "lastName", nuovo.cognome(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(credenziale));

        URI location;
        try {
            location = restClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(utente)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();
        } catch (RestClientResponseException e) {
            int stato = e.getStatusCode().value();
            if (stato == 409) {
                // niente dettagli dal corpo di Keycloak nel messaggio: il chiamante sa gia'
                // quale email ha inviato, e il corpo puo' rivelare quale campo ha fatto conflitto
                throw new UtenteGiaEsistenteException("Esiste già un utente registrato con questa email");
            }
            if (stato == 403) {
                log.error("Il service account '{}' non è autorizzato a creare utenti nel realm '{}': "
                        + "assegnargli il ruolo client 'manage-users' di realm-management", clientId, realm);
            } else {
                log.error("Creazione dell'utente su Keycloak fallita (HTTP {})", stato);
            }
            throw new RegistrazioneNonDisponibileException("creazione dell'utente su Keycloak fallita");
        } catch (ResourceAccessException e) {
            throw keycloakIrraggiungibile(e);
        }

        if (location == null) {
            log.error("Keycloak non ha restituito l'header Location dopo la creazione dell'utente");
            throw new RegistrazioneNonDisponibileException("id dell'utente Keycloak non determinabile");
        }

        String percorso = location.getPath();
        String keycloakId = percorso.substring(percorso.lastIndexOf('/') + 1);
        if (!StringUtils.hasText(keycloakId)) {
            log.error("Header Location inatteso dopo la creazione dell'utente: {}", location);
            throw new RegistrazioneNonDisponibileException("id dell'utente Keycloak non determinabile");
        }
        return keycloakId;
    }

    /** Assegna un ruolo realm all'utente appena creato. */
    public void assegnaRuoloRealm(String token, String keycloakId, RuoloRealm ruolo) {
        List<Map<String, String>> corpo = List.of(Map.of("id", ruolo.id(), "name", ruolo.nome()));

        esegui(
                () -> restClient.post()
                        .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, keycloakId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(corpo)
                        .retrieve()
                        .toBodilessEntity(),
                stato -> {
                    log.error("Assegnazione del ruolo realm '{}' all'utente {} fallita (HTTP {})",
                            ruolo.nome(), keycloakId, stato);
                    return new RegistrazioneNonDisponibileException("assegnazione del ruolo realm fallita");
                });
    }

    /**
     * Cancella un utente Keycloak senza propagare errori: serve a compensare una registrazione
     * interrotta a meta'. Se anche la compensazione fallisce si logga e basta, perche'
     * l'errore originale e' piu' importante e non va sostituito da quello della pulizia.
     */
    public void eliminaUtenteSenzaPropagareErrori(String token, String keycloakId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
            log.warn("Registrazione interrotta: utente Keycloak {} rimosso per compensazione", keycloakId);
        } catch (RuntimeException e) {
            log.error("Compensazione fallita: l'utente Keycloak {} è rimasto orfano e va rimosso a mano "
                    + "dalla console admin", keycloakId, e);
        }
    }

    private <T> T esegui(java.util.function.Supplier<T> chiamata,
                         java.util.function.Function<Integer, RegistrazioneNonDisponibileException> suErroreHttp) {
        try {
            return chiamata.get();
        } catch (RestClientResponseException e) {
            throw suErroreHttp.apply(e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw keycloakIrraggiungibile(e);
        }
    }

    private RegistrazioneNonDisponibileException keycloakIrraggiungibile(ResourceAccessException e) {
        log.error("Keycloak non raggiungibile durante la registrazione: {}", e.getMessage());
        return new RegistrazioneNonDisponibileException("Keycloak non raggiungibile", e);
    }
}
