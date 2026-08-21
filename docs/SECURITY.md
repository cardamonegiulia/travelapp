# Security hardening — travelapp-backend

Riepilogo delle modifiche di sicurezza applicate su `dev/experience`, in 6 fasi (vedi storia
commit su questo branch). Copre: autorizzazione a livello di oggetto (BOLA/IDOR), validazione
input, protezione delle risorse, gestione degli errori, audit logging, sicurezza di rete.

## Perché

Prima di questo lavoro, `SecurityConfig` permetteva a qualunque utente autenticato di accedere
a qualsiasi endpoint (nessun controllo di ruolo, nessun controllo di ownership). In pratica:
un utente poteva leggere/pagare/annullare le prenotazioni di chiunque, leggere/modificare/
cancellare il profilo di un altro utente, creare prenotazioni a nome di terzi, cancellare
itinerari altrui. Il `JwtAuthenticationConverter` inoltre non estraeva correttamente i ruoli
dal token Keycloak (claim annidati non risolti dal converter standard), quindi anche
aggiungendo `@PreAuthorize` non avrebbe funzionato senza il fix.

## Cosa è cambiato

### Autorizzazione (Fase 1)
- `SecurityFilterChain` deny-by-default: allow-list esplicita di rotte pubbliche, tutto il
  resto sotto `/api/**` richiede autenticazione, qualunque rotta non mappata è negata.
- `@EnableMethodSecurity` + `@PreAuthorize` sui controller, per ruolo minimo necessario.
- `KeycloakRoleConverter` custom: legge sia `realm_access.roles` sia
  `resource_access.<client-id>.roles` (claim annidati, non risolti dal converter standard).
- `AudienceValidator` + `JwtValidators.createDefaultWithIssuer`: issuer e audience del token
  validati, nessun default permissivo.
- BOLA/IDOR chiusi: l'identità utente è sempre presa dal token (`sub`), mai da parametri
  client. Query filtrate per ownership (`findByIdAndViaggiatoreId`, ecc.): se la risorsa non è
  tua, **404** (non 403), per non rivelare l'esistenza dell'id a chi non ne ha diritto.
  Permission evaluator dedicato (`@utenteSecurity.isSelf(...)`) per i controlli "self or admin".

### Input/output (Fase 2)
- DTO di request separati da quelli di response; nessuna entità JPA nelle firme dei controller.
- Bean validation (`@NotNull`, `@Positive`, `@Size`, ecc.) su tutti i DTO di request.
- `spring.jackson.deserialization.fail-on-unknown-properties=true`: un campo "di sistema"
  iniettato nel payload (es. `organizzatoreId`) fa fallire la richiesta con 400, invece di
  essere ignorato silenziosamente.

### Protezione delle risorse (Fase 3)
- Rate limiting in-memory (Bucket4j): per utente autenticato (`sub` del token) e per IP per le
  richieste anonime. 429 con header `Retry-After`/`X-RateLimit-*`.
- Paginazione obbligatoria su tutte le collection esposte, con tetto massimo lato server
  (`spring.data.web.pageable.max-page-size=100`): un client non può forzare `size=10000`.
- Limiti multipart e timeout su transazioni/query.
- Tetto sul lavoro generato da una singola richiesta: `POST /api/attivita/con-sessioni` crea
  una sessione per ogni giorno dell'intervallo richiesto, quindi l'intervallo è limitato a
  366 giorni e i giorni della settimana sono validati (1-7, al massimo 7). Senza, una sola
  chiamata chiedeva milioni di insert in un'unica transazione.

### Gestione degli errori (Fase 4)
- Risposte di errore in formato RFC 7807 (`ProblemDetail`): `type`, `title`, `status`,
  `detail`, `instance`, più `traceId` (correlation id) per la ricerca nei log.
- Handler per tutte le eccezioni applicative, incluse quelle prima non gestite
  (`IllegalStateException`, `IllegalArgumentException`, `DataIntegrityViolationException`,
  `EntityNotFoundException`) che cadevano tutte nel fallback generico 500.
- Nessuno stack trace o dettaglio infrastrutturale (nomi tabella/colonna/constraint) nel body
  della risposta: solo nei log server. Baseline sicura di default
  (`server.error.include-stacktrace=never`, `include-message=never`); profilo `dev` opzionale
  per rilassare in locale, profilo `prod` che ribadisce i vincoli.

### Audit logging (Fase 5)
- JPA Auditing (`@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`/`@LastModifiedBy`) su Utente,
  Itinerario, SingolaAttivita, Prenotazione, Pagamento, Recensione, Preferito — popolato dal
  `sub` del token, mai da input client.
- Audit log applicativo su logger dedicato (`AUDIT`, file separato `logs/audit.log`, JSON,
  `additivity=false`) per creazione/modifica/cancellazione di risorse, accessi negati e
  autenticazioni fallite. Ogni evento: chi (subject+username), quando (UTC), cosa, risorsa
  (tipo+id), esito, IP, traceId. **Mai** token JWT completo, password o header `Authorization`.

### Rete e configurazione (Fase 6)
- Header di sicurezza: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, HSTS,
  `Referrer-Policy: strict-origin-when-cross-origin`, CSP (`default-src 'self'`, con
  `unsafe-inline` solo per gli asset bundled di swagger-ui, disabilitato comunque in prod).
- CORS: allow-list esplicita configurabile (`CORS_ALLOWED_ORIGINS`), nessuna credenziale
  cross-origin (`allowCredentials=false`, coerente con bearer token anziché cookie).
- HTTPS obbligatorio in produzione (`app.security.require-https=true` nel profilo `prod`,
  redirect automatico), `server.forward-headers-strategy=framework` per funzionare dietro un
  reverse proxy che termina il TLS.
- Controllo fail-fast all'avvio (solo profilo `prod`): se l'issuer Keycloak non è HTTPS,
  l'applicazione non parte.
- CSRF disabilitato con motivazione esplicita in codice: API stateless a bearer token, nessun
  cookie di sessione — se in futuro si introduce un flusso a cookie, va riabilitato.

## Mappa endpoint → ruoli/scope richiesti

| Endpoint | Metodo | Autorizzazione |
|---|---|---|
| `/api/auth/registrazione` | POST | **pubblico** (unica rotta anonima sotto `/api`) |
| `/api/utenti` | POST | `ADMIN` |
| `/api/utenti` | GET | `ADMIN` |
| `/api/utenti/{id}` | GET/PUT/DELETE | `ADMIN` oppure proprietario (`self`) |
| `/api/utenti/me` | POST | autenticato (qualunque ruolo) |
| `/api/utenti/me/password` | POST | autenticato **con autenticazione recente** (`auth_time` entro `app.security.max-auth-age-seconds`); 401 + `WWW-Authenticate` altrimenti |
| `/api/itinerari` | GET | autenticato |
| `/api/itinerari/{id}` | GET | autenticato |
| `/api/itinerari` | POST | `ORGANIZZATORE` o `ADMIN` |
| `/api/itinerari/{id}` | DELETE | `ORGANIZZATORE` (solo i propri) o `ADMIN` |
| `/api/attivita` | GET | autenticato |
| `/api/attivita/{id}` | GET | autenticato |
| `/api/itinerari/{id}/immagini` | POST | `ORGANIZZATORE` proprietario o `ADMIN` (404 se non tuo) |
| `/api/itinerari/{id}/immagini` | GET | autenticato |
| `/api/itinerari/{id}/immagini/{immagineId}` | DELETE | `ORGANIZZATORE` proprietario o `ADMIN` |
| `/api/attivita/con-sessioni` | POST | `ORGANIZZATORE` o `ADMIN` |
| `/api/attivita/{id}` | DELETE | `ORGANIZZATORE` (solo le proprie) o `ADMIN` |
| `/api/prenotazioni` | POST | autenticato (crea per sé stesso) |
| `/api/prenotazioni/{id}` | GET | proprietario o `ADMIN` (404 se non tua) |
| `/api/prenotazioni/utente/{utenteId}` | GET | proprietario o `ADMIN` (403 se non tuo) |
| `/api/prenotazioni/{id}/paga` | POST | proprietario o `ADMIN` |
| `/api/prenotazioni/{id}/annulla` | POST | proprietario o `ADMIN` |
| `/api/preferiti` | GET/POST/DELETE | autenticato (sempre sui propri preferiti) |
| `/api/recensioni/{id}` | GET | autenticato |
| `/api/recensioni/itinerario/{id}` | GET | autenticato |
| `/api/recensioni/itinerario/{id}/media` | GET | autenticato |
| `/api/recensioni` | POST | autenticato (a proprio nome) |
| `/api/recensioni/{id}` | DELETE | autore (403 se non tua) |
| `/api/recensioni/{id}/immagini` | POST | autore della recensione o `ADMIN` (403 se non tua) |
| `/api/recensioni/{id}/immagini` | GET | autenticato |
| `/api/recensioni/{id}/immagini/{immagineId}` | DELETE | autore della recensione o `ADMIN` |
| `/api/immagini` | POST | autenticato (carica a proprio nome) |
| `/api/immagini/{id}` | GET | autenticato |
| `/api/immagini/{id}/contenuto` | GET | autenticato |
| `/api/immagini/mie` | GET | autenticato (solo le proprie) |
| `/api/immagini/{id}` | DELETE | proprietario o `ADMIN` (404 se non tua) |
| `/swagger-ui/**`, `/v3/api-docs/**` | GET | pubblico (**disabilitato** in prod) |
| `/actuator/health`, `/actuator/info` | GET | pubblico |
| `/actuator/**` (altri) | — | `ADMIN` (nessun endpoint actuator è oggi sul classpath) |

## Configurazione Keycloak necessaria

Dettagliata in `docs/keycloak-setup.md`. In sintesi, va applicata manualmente sull'istanza
Keycloak (non dal codice):

1. **Audience mapper** sul client `travelapp-backend` — senza questo, la validazione
   dell'audience (Fase 1c) rifiuta tutti i token con 401.
2. **Ruolo realm `ADMIN`**, assegnato agli utenti amministrativi.
3. In produzione: `issuer-uri`/`jwk-set-uri` devono puntare a un endpoint Keycloak HTTPS
   (verificato anche a runtime, fail-fast se non lo è).

## Variabili d'ambiente rilevanti

| Variabile | Scopo | Default |
|---|---|---|
| `OAUTH2_ISSUER_URI` / `OAUTH2_JWK_SET_URI` | Endpoint Keycloak | `http://localhost:8090/realms/travelapp` |
| `SECURITY_EXPECTED_AUDIENCE` / `SECURITY_RESOURCE_CLIENT_ID` | Audience/client id attesi nel token | `travelapp-backend` |
| `CORS_ALLOWED_ORIGINS` | Origin del/i frontend web ammessi (allow-list, comma-separated) | `http://localhost:3000` (placeholder dev, **da impostare per il frontend reale**) |
| `RATELIMIT_AUTHENTICATED_CAPACITY` / `RATELIMIT_ANONYMOUS_CAPACITY` | Richieste/minuto per utente/IP | `60` / `20` |
| `SPRING_PROFILES_ACTIVE` | `dev` per debug locale, `prod` per i vincoli di produzione | nessuno (baseline sicura) |

## Checklist infrastrutturale (fuori dal perimetro del codice)

- [ ] **TLS termination**: reverse proxy/load balancer davanti al backend con certificato
      valido; il backend stesso non gestisce TLS (si affida a `forward-headers-strategy`).
- [ ] **WAF** davanti all'API, se esposta pubblicamente.
- [ ] **Secret management**: `DB_PASSWORD`, credenziali Keycloak, ecc. oggi passate via
      variabili d'ambiente — in produzione andrebbero da un vault/secret manager, non da un
      `.env` in chiaro o da variabili impostate manualmente sull'host.
- [ ] **Credenziali Keycloak admin** (`docker-compose.yml`, `admin`/`admin`) sono solo per
      sviluppo locale: da rigenerare per qualunque ambiente condiviso.
- [ ] **Rate limiting distribuito**: l'implementazione attuale è in-memory, per singola
      istanza. Se il backend scala orizzontalmente, va sostituita con un backend condiviso
      (es. Redis) o spostata a livello di gateway.
- [ ] **Backup e disaster recovery** del database Postgres.
- [ ] **Monitoring/alerting** sul file `logs/audit.log` (es. spike di `ACCESSO_NEGATO` o
      `AUTENTICAZIONE_FALLITA` da uno stesso IP/subject).
- [ ] Verifica end-to-end (non eseguita in questo lavoro per assenza di un DB/Keycloak
      raggiungibili nell'ambiente di sviluppo usato): avviare `docker-compose up`, applicare
      l'audience mapper Keycloak, e testare manualmente i flussi principali con Postman/curl.
