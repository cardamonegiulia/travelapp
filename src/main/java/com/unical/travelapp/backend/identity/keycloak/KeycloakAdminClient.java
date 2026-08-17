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

    /** I campi del profilo che esistono in entrambe le fonti e vanno tenuti allineati. */
    public record ProfiloKeycloak(String email, String nome, String cognome) {
    }

    /**
     * Access token del service account (grant {@code client_credentials}).
     *
     * <p>Deliberatamente non messo in cache: le operazioni amministrative sono rare, e un
     * token in cache introdurrebbe il caso "token valido secondo noi ma rifiutato da Keycloak"
     * dopo un riavvio del realm, con una gestione del refresh da mantenere.
     *
     * <p>Solleva la superclasse {@link IdentityProviderNonDisponibileException} e non la
     * variante della registrazione: questo passo e' comune a tutte le operazioni sull'IdP.
     */
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
     * @throws PasswordNonConformeException se il realm rifiuta la password per policy: come in
     *         {@link #impostaPassword}, e' un errore del chiamante e non un guasto del servizio
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
                // L'indirizzo non e' verificato: nessuno ha ancora dimostrato di poterlo
                // leggere. Dichiararlo verificato perche' e' arrivato in un form significa
                // permettere a chiunque di registrarsi con l'email di un altro, e di
                // ricevere da quel momento le comunicazioni destinate a quella persona.
                // Con "verifyEmail" attivo sul realm, Keycloak invia la mail di verifica al
                // primo tentativo di login e non rilascia token finche' non e' confermata.
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
                // niente dettagli dal corpo di Keycloak nel messaggio: il chiamante sa gia'
                // quale email ha inviato, e il corpo puo' rivelare quale campo ha fatto conflitto
                throw new UtenteGiaEsistenteException("Esiste già un utente registrato con questa email");
            }
            if (stato == 400) {
                // Delle cose che l'utente ha scritto, la password e' l'unica che il realm
                // valuta per conto suo: formato dell'email, nome e cognome li ha gia' validati
                // il DTO prima di arrivare qui. Un 400 in creazione e' quindi la policy del
                // realm che respinge la password, lo stesso significato che ha in
                // impostaPassword. Senza questo ramo diventava un 503 "servizio non
                // disponibile": chi si registra rileggeva la stessa password all'infinito
                // senza sapere che il problema era quella.
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

    /**
     * Allinea su Keycloak i campi del profilo modificati in locale.
     *
     * <p>Vengono inviati solo email, nome e cognome: Keycloak applica i campi presenti nella
     * rappresentazione e lascia intatti gli altri, quindi {@code enabled}, credenziali e ruoli
     * non vengono toccati da qui.
     *
     * <p>Quando l'email cambia viene aggiunto {@code emailVerified: false}, ed e' la ragione
     * per cui il metodo vuole saperlo. La verifica dimostra che chi possiede l'account sa
     * leggere <b>quell'</b> indirizzo: portarsela dietro sul nuovo significherebbe permettere a
     * chiunque, dopo essersi verificato su una casella propria, di spostare l'account
     * sull'indirizzo di un'altra persona e risultare verificato su di esso. Con
     * {@code verifyEmail} attivo sul realm, Keycloak chiede la nuova conferma al login
     * successivo. Quando l'email non cambia il campo non viene inviato affatto: una
     * rappresentazione che non lo contiene lascia intatto il valore su Keycloak.
     *
     * <p>Lo {@code username} non viene modificato di proposito, nemmeno quando cambia l'email.
     * Per gli account nati dalla registrazione self-service i due valori coincidono, ma per
     * quelli creati in console possono essere diversi, e riscrivere lo username significa
     * cambiare l'identificativo con cui una persona fa login. Con
     * {@code loginWithEmailAllowed} attivo sul realm, l'accesso con la nuova email funziona
     * comunque.
     *
     * @throws UtenteGiaEsistenteException se l'email e' gia' di un altro utente del realm
     * @throws IdentityProviderNonDisponibileException se Keycloak non risponde o rifiuta
     */
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

    /**
     * Ripristina su Keycloak il profilo precedente senza propagare errori: compensa un
     * aggiornamento riuscito sull'IdP ma non salvato in locale. Come per la registrazione,
     * l'errore originale e' quello che deve arrivare al chiamante, non quello della pulizia.
     *
     * <p>Ripristina l'indirizzo, non il suo stato di verifica: {@code emailVerified} resta a
     * {@code false}, quindi l'utente dovra' riconfermare la casella da cui era partito. E'
     * voluto — qui non si sa se quell'indirizzo fosse verificato (potrebbe non esserlo mai
     * stato), e leggerlo prima di ogni aggiornamento costerebbe una chiamata in piu' su ogni
     * modifica di profilo per un caso che si presenta solo se la scrittura locale fallisce.
     * Chiedere una verifica di troppo e' il lato giusto in cui sbagliare.
     */
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
     * Sostituisce la password dell'utente.
     *
     * <p>La credenziale non e' temporanea: una password temporanea farebbe scattare l'azione
     * richiesta UPDATE_PASSWORD al login successivo, cioe' chiederebbe all'utente di
     * cambiare di nuovo quella che ha appena scelto.
     *
     * @throws PasswordNonConformeException se il realm rifiuta la password per policy: e' un
     *         400, non un guasto del servizio
     * @throws IdentityProviderNonDisponibileException per qualunque altro errore dell'IdP
     */
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
                // l'unica parte della richiesta che dipende dall'utente e' la password:
                // un 400 qui e' la policy del realm che la respinge
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

    /**
     * Chiude tutte le sessioni attive dell'utente, senza propagare errori.
     *
     * <p>Va chiamata dopo un cambio password: senza, chi avesse gia' rubato un token o una
     * sessione manterrebbe l'accesso: proprio la ragione per cui la vittima sta cambiando la
     * password. Non blocca l'operazione se fallisce, perche' la password nuova e' comunque
     * gia' attiva e il rischio residuo e' limitato alla durata dei token in circolazione.
     */
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

    /**
     * Cancella un utente Keycloak propagando gli errori: e' la cancellazione "vera", quella
     * chiesta da un amministratore, e deve fallire in modo visibile.
     *
     * <p>Un utente gia' assente su Keycloak (404) non e' un errore: l'operazione e'
     * idempotente e il chiamante puo' comunque rimuovere il record locale. E' il caso di un
     * account cancellato a mano dalla console, che altrimenti resterebbe impossibile da
     * ripulire in locale.
     *
     * @throws IdentityProviderNonDisponibileException se Keycloak non risponde o rifiuta la
     *         cancellazione: il record locale non va rimosso, o si tornerebbe alla divergenza
     *         che questa chiamata serve a evitare
     */
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
