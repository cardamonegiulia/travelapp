package com.unical.travelapp.backend.identity.keycloak;

import com.unical.travelapp.backend.identity.exception.IdentityProviderNonDisponibileException;
import com.unical.travelapp.backend.identity.exception.PasswordNonConformeException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public record RuoloRealm(String id, String nome) {
    }

    public record NuovoUtenteKeycloak(String username, String email, String nome, String cognome, String password) {
    }

    public record ProfiloKeycloak(String email, String nome, String cognome) {
    }

    public String ottieniTokenAmministrativo() {
        if (!StringUtils.hasText(clientSecret)) {
            log.error("Operazioni amministrative su Keycloak non disponibili: 'app.keycloak.admin.client-secret' "
                    + "non è configurato (variabile d'ambiente KEYCLOAK_ADMIN_CLIENT_SECRET)");
            throw new IdentityProviderNonDisponibileException("client secret del service account non configurato");
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
                    return new IdentityProviderNonDisponibileException("service account Keycloak non autenticato");
                });

        Object accessToken = risposta == null ? null : risposta.get("access_token");
        if (accessToken == null) {
            log.error("Risposta del token endpoint senza campo access_token per il client '{}'", clientId);
            throw new IdentityProviderNonDisponibileException("risposta del token endpoint priva di access_token");
        }
        return accessToken.toString();
    }

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

    public String creaUtente(String token, NuovoUtenteKeycloak nuovo) {
        Map<String, Object> credenziale = Map.of(
                "type", "password",
                "value", nuovo.password(),
                "temporary", false);

        Map<String, Object> utente = Map.of(
                "username", nuovo.username(),
                "email", nuovo.email(),
                "firstName", nuovo.nome(),
                "lastName", nuovo.cognome(),
                "enabled", true,
                "emailVerified", false,
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
                throw new UtenteGiaEsistenteException("Esiste già un utente registrato con questa email");
            }
            if (stato == 400) {
                log.info("Registrazione rifiutata dalla policy password del realm '{}'", realm);
                throw new PasswordNonConformeException("La password non rispetta i requisiti richiesti");
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

    public void aggiornaProfilo(String token, String keycloakId, ProfiloKeycloak profilo, boolean emailCambiata) {
        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpoProfilo(profilo, emailCambiata))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int stato = e.getStatusCode().value();
            if (stato == 409) {
                throw new UtenteGiaEsistenteException("Esiste già un utente registrato con questa email");
            }
            if (stato == 404) {
                log.error("Utente Keycloak {} non trovato: il record locale punta a un'identita' che non esiste "
                        + "piu' sull'IdP, il profilo non e' allineabile", keycloakId);
            } else if (stato == 403) {
                log.error("Il service account '{}' non è autorizzato ad aggiornare utenti nel realm '{}': "
                        + "assegnargli il ruolo client 'manage-users' di realm-management", clientId, realm);
            } else {
                log.error("Aggiornamento del profilo Keycloak {} fallito (HTTP {})", keycloakId, stato);
            }
            throw new IdentityProviderNonDisponibileException("aggiornamento del profilo su Keycloak fallito");
        } catch (ResourceAccessException e) {
            log.error("Keycloak non raggiungibile durante l'aggiornamento del profilo {}: {}",
                    keycloakId, e.getMessage());
            throw new IdentityProviderNonDisponibileException("Keycloak non raggiungibile", e);
        }
    }

    public void aggiornaProfiloSenzaPropagareErrori(String token, String keycloakId, ProfiloKeycloak profilo) {
        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpoProfilo(profilo, false))
                    .retrieve()
                    .toBodilessEntity();
            log.warn("Aggiornamento non salvato in locale: profilo Keycloak {} riportato ai valori precedenti",
                    keycloakId);
        } catch (RuntimeException e) {
            log.error("Compensazione fallita: il profilo Keycloak {} resta disallineato dal record locale "
                    + "e va corretto a mano dalla console admin", keycloakId, e);
        }
    }

    private Map<String, Object> corpoProfilo(ProfiloKeycloak profilo, boolean azzeraVerificaEmail) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("email", profilo.email());
        corpo.put("firstName", profilo.nome());
        corpo.put("lastName", profilo.cognome());
        if (azzeraVerificaEmail) {
            corpo.put("emailVerified", false);
        }
        return corpo;
    }

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

    public void impostaPassword(String token, String keycloakId, String nuovaPassword) {
        Map<String, Object> credenziale = Map.of(
                "type", "password",
                "value", nuovaPassword,
                "temporary", false);

        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credenziale)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int stato = e.getStatusCode().value();
            if (stato == 400) {
                log.info("Password rifiutata dalla policy del realm per l'utente {}", keycloakId);
                throw new PasswordNonConformeException("La password non rispetta i requisiti richiesti");
            }
            if (stato == 403) {
                log.error("Il service account '{}' non è autorizzato a cambiare le password nel realm '{}': "
                        + "assegnargli il ruolo client 'manage-users' di realm-management", clientId, realm);
            } else {
                log.error("Cambio password dell'utente Keycloak {} fallito (HTTP {})", keycloakId, stato);
            }
            throw new IdentityProviderNonDisponibileException("cambio password su Keycloak fallito");
        } catch (ResourceAccessException e) {
            log.error("Keycloak non raggiungibile durante il cambio password dell'utente {}: {}",
                    keycloakId, e.getMessage());
            throw new IdentityProviderNonDisponibileException("Keycloak non raggiungibile", e);
        }
    }

    public void terminaSessioniSenzaPropagareErrori(String token, String keycloakId) {
        try {
            restClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/logout", realm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.error("Sessioni dell'utente Keycloak {} non terminate dopo il cambio password: "
                    + "i token gia' emessi restano validi fino alla scadenza", keycloakId, e);
        }
    }

    public void eliminaUtente(String token, String keycloakId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int stato = e.getStatusCode().value();
            if (stato == 404) {
                log.warn("Utente Keycloak {} gia' assente: si procede con la sola cancellazione locale", keycloakId);
                return;
            }
            if (stato == 403) {
                log.error("Il service account '{}' non è autorizzato a cancellare utenti nel realm '{}': "
                        + "assegnargli il ruolo client 'manage-users' di realm-management", clientId, realm);
            } else {
                log.error("Cancellazione dell'utente Keycloak {} fallita (HTTP {})", keycloakId, stato);
            }
            throw new IdentityProviderNonDisponibileException("cancellazione dell'utente su Keycloak fallita");
        } catch (ResourceAccessException e) {
            log.error("Keycloak non raggiungibile durante la cancellazione dell'utente {}: {}",
                    keycloakId, e.getMessage());
            throw new IdentityProviderNonDisponibileException("Keycloak non raggiungibile", e);
        }
    }

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
                         java.util.function.Function<Integer, ? extends IdentityProviderNonDisponibileException> suErroreHttp) {
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
