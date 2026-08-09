# Inventario di sicurezza — stato reale del codice

Ricostruito leggendo il codice sul branch `dev/experience`, non dalla descrizione di
progetto. È la base su cui sono scritti i test in `src/test/java/.../security/`.

Data: 2026-08-05 · Commit di partenza: `15fdf2d`

---

## 1. Catena di filtri (`config/SecurityConfig.java`)

Ordine e contenuto del `SecurityFilterChain`:

| Elemento | Valore effettivo |
|---|---|
| CSRF | disabilitato (scelta esplicita: API stateless a bearer token) |
| CORS | `corsConfigurationSource()`, registrata **solo su `/api/**`** |
| Sessione | `SessionCreationPolicy.STATELESS` |
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| HSTS | `max-age=31536000`, `includeSubDomains`; scritto **solo su richieste HTTPS** |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| CSP | `default-src 'self'; frame-ancestors 'none'; object-src 'none'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'` |
| Rate limit | `addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class)` |
| HTTPS obbligatorio | `redirectToHttps()` solo se `app.security.require-https=true` (profilo prod) |

Regole di autorizzazione, **nell'ordine in cui sono valutate**:

1. `/swagger-ui/**`, `/v3/api-docs/**` → `permitAll()`
2. `/actuator/health`, `/actuator/info` → `permitAll()`
3. `/actuator/**` → `hasRole('ADMIN')`
4. `/api/**` → `authenticated()`
5. `anyRequest()` → `denyAll()`

CORS: origini da `app.security.cors.allowed-origins`, metodi
`GET, POST, PUT, DELETE, OPTIONS`, header `Authorization, Content-Type`,
`allowCredentials=false`, `maxAge=3600`.

## 2. Validazione del token

- `JwtDecoder`: `NimbusJwtDecoder.withJwkSetUri(...)` (risoluzione **lazy**: il contesto
  parte senza contattare Keycloak).
- Validator: `DelegatingOAuth2TokenValidator(JwtValidators.createDefaultWithIssuer(issuer),
  AudienceValidator(expectedAudience))` → issuer + scadenza + audience.
- `KeycloakRoleConverter(resourceClientId)`: legge `realm_access.roles` e
  `resource_access.<client-id>.roles`, prefissa `ROLE_`. Ignora `scope`, `roles`,
  `authorities`, `groups` e i ruoli di altri client.
- `@EnableMethodSecurity` attivo; permission evaluator `@utenteSecurity.isSelf(#id, authentication)`.

## 3. Mappa endpoint → autorizzazione → controllo di ownership

`*` = qualunque utente autenticato. Tutte le rotte stanno sotto `/api/**`, quindi
l'anonimo riceve sempre 401 — con l'unica eccezione della registrazione self-service,
esplicitamente `permitAll` e limitata al metodo POST.

| Metodo | Rotta | Autorizzazione | Ownership | Status per risorsa altrui |
|---|---|---|---|---|
| POST | `/api/auth/registrazione` | **anonimo** (`permitAll`) | ruolo scelto dal richiedente, solo VIAGGIATORE/ORGANIZZATORE | n/d |
| POST | `/api/utenti` | `hasRole('ADMIN')` | — | 403 se non admin |
| GET | `/api/utenti` | `hasRole('ADMIN')` | — | 403 se non admin |
| GET | `/api/utenti/{id}` | ADMIN **o** `isSelf(#id)` | permission evaluator | **403** |
| PUT | `/api/utenti/{id}` | ADMIN **o** `isSelf(#id)` | permission evaluator | **403** |
| DELETE | `/api/utenti/{id}` | ADMIN **o** `isSelf(#id)` | permission evaluator | **403** |
| POST | `/api/utenti/me` | `*` | identità dal claim `sub` | n/d |
| GET | `/api/itinerari` | `*` | — (catalogo pubblico agli autenticati) | n/d |
| GET | `/api/itinerari/{id}` | `*` | — | n/d |
| POST | `/api/itinerari` | `ORGANIZZATORE, ADMIN` | organizzatore = utente di sessione | n/d |
| DELETE | `/api/itinerari/{id}` | `ORGANIZZATORE, ADMIN` | query filtrata `findByIdAndOrganizzatore_Id` | **404** |
| GET | `/api/attivita` | `*` | — | n/d |
| GET | `/api/attivita/{id}` | `*` | — | n/d |
| POST | `/api/attivita/con-sessioni` | `ORGANIZZATORE, ADMIN` | organizzatore = utente di sessione | n/d |
| DELETE | `/api/attivita/{id}` | `ORGANIZZATORE, ADMIN` | query filtrata `findByIdAndOrganizzatore_Id` | **404** |
| POST | `/api/prenotazioni` | `*` | viaggiatore = utente di sessione | n/d |
| GET | `/api/prenotazioni/{id}` | `*` | query filtrata `findByIdAndViaggiatoreId` | **404** |
| GET | `/api/prenotazioni/utente/{utenteId}` | `*` | confronto esplicito → `AccessDeniedException` | **403** |
| POST | `/api/prenotazioni/{id}/paga` | `*` | via `getPrenotazioneById` | **404** |
| POST | `/api/prenotazioni/{id}/annulla` | `*` | via `getPrenotazioneById` | **404** |
| GET | `/api/preferiti` | `*` | sempre l'utente di sessione | n/d |
| POST | `/api/preferiti` | `*` | sempre l'utente di sessione | n/d |
| DELETE | `/api/preferiti` | `*` | sempre l'utente di sessione | n/d |
| GET | `/api/recensioni/{id}` | `*` | **nessuno** (contenuto pubblico) | 200 |
| GET | `/api/recensioni/itinerario/{id}` | `*` | nessuno (contenuto pubblico) | 200 |
| GET | `/api/recensioni/itinerario/{id}/media` | `*` | nessuno | 200 |
| POST | `/api/recensioni` | `*` | autore = utente di sessione; prenotazione verificata | **403** |
| DELETE | `/api/recensioni/{id}` | `*` | confronto esplicito → `AccessDeniedException` | **403** |
| — | `/api/pagamenti` | `*` | controller **senza endpoint** | 404 |

Nota sulla scelta 404 vs 403: dove il servizio usa una query filtrata per proprietario si
ottiene 404 (non si rivela l'esistenza dell'id); dove il controllo è un confronto esplicito
o un `@PreAuthorize` si ottiene 403. In entrambi i casi i test verificano che **id altrui e
id inesistente producano lo stesso status**, quindi non c'è enumerazione.

## 4. Gestione degli errori (`exception/GlobalExceptionHandler.java`)

| Eccezione | Status | `type` |
|---|---|---|
| `UtenteGiaEsistenteException` | 409 | `risorsa-esistente` |
| `UtenteNonTrovatoException` | 404 | `risorsa-non-trovata` |
| `MethodArgumentNotValidException` | 400 | `validazione-fallita` (+ mappa `errori`) |
| `RichiestaPrenotazioneNonValidaException` | 400 | `richiesta-non-valida` |
| `AttivitaExtraNonValidaException` | 400 | `attivita-extra-non-valida` |
| `DisponibilitaNonTrovataException` | 404 | `risorsa-non-trovata` |
| `PrenotazioneNonTrovataException` / `PrenotazioneNonTrovata` | 404 | `risorsa-non-trovata` |
| `PagamentoNonTrovatoException` | 404 | `risorsa-non-trovata` |
| `RecensioneNonTrovata`, `ItinerarioNonTrovato(Exception)`, `SingolaAttivitaNonTrovataException` | 404 | `risorsa-non-trovata` |
| `PostiInsufficientiException` | 409 | `posti-insufficienti` |
| `StatoPrenotazioneNonValidoException` / `IllegalStateException` | 409 | `stato-non-valido` |
| `HttpMessageNotReadableException` | 400 | `payload-non-valido` |
| `EntityNotFoundException` | 404 | `risorsa-non-trovata` |
| `DataIntegrityViolationException` | 409 | `conflitto-dati` |
| `IllegalArgumentException` | 400 | `richiesta-non-valida` |
| `AccessDeniedException` | 403 | `accesso-negato` (+ evento di audit) |
| `AuthenticationException` | 401 | `non-autenticato` (+ evento di audit) |
| `Exception` | 500 | `errore-interno` |
| **`HttpRequestMethodNotSupportedException`** | 405 | `metodo-non-consentito` |
| **`HttpMediaTypeNotSupportedException`** | 415 | `formato-non-supportato` |
| **`HttpMediaTypeNotAcceptableException`** | 406 | `formato-non-accettabile` |
| **`NoResourceFoundException`** | 404 | `risorsa-non-trovata` |
| **`MaxUploadSizeExceededException`** | 413 | `contenuto-troppo-grande` |
| **`MethodArgumentTypeMismatchException` / `MissingServletRequestParameterException`** | 400 | `parametro-non-valido` |
| **`PropertyReferenceException`** | 400 | `ordinamento-non-valido` |

In grassetto: handler **aggiunti da questo lavoro** (vedi finding F-02 nel report).
Ogni ProblemDetail porta `type`, `title`, `status`, `detail`, `instance`, `traceId`.

## 5. Rate limiting (`config/RateLimitFilter.java`)

- Bucket4j in-memory, `ConcurrentHashMap` (**non condiviso fra istanze**).
- Chiave: `user:<sub>` se autenticato, altrimenti `ip:<request.getRemoteAddr()>`.
  **Non** usa `X-Forwarded-For** — corretto in assenza di reverse proxy fidato.
- Capienza: `app.ratelimit.authenticated.capacity` (60/min), `...anonymous.capacity` (20/min).
- Header emessi: `X-RateLimit-Limit` sempre, `X-RateLimit-Remaining` sempre,
  `Retry-After` sul 429.
- Corpo del 429: `{"status":429,"errore":"..."}` con `Content-Type: application/json`
  — **non** un ProblemDetail (vedi finding F-04).

## 6. Audit

- `common/audit/Auditable.java`: `@MappedSuperclass` con `creatoIl`, `modificatoIl`,
  `creatoDa`, `modificatoDa`. Esteso da `Utente`, `Prenotazione`, `Pagamento`,
  `Itinerario`, `SingolaAttivita`, `Recensione`, `Preferito`.
- `SecurityAuditorAware`: autore = `jwt.getSubject()`, `"system"` se non autenticato.
- `AuditLogger`: logger dedicato `"AUDIT"`, evento JSON con
  `timestamp, traceId, subject, username, azione, risorsaTipo, risorsaId, esito, ip[, motivo]`.
- `logback-spring.xml`: appender `AUDIT_FILE` su `logs/audit.log`, rolling giornaliero,
  `maxHistory=90`, `additivity=false`.
- `CorrelationIdFilter`: `@Order(HIGHEST_PRECEDENCE)`, header `X-Correlation-Id`,
  MDC `traceId`, propaga l'id fornito dal chiamante.

## 7. Configurazione per profilo

| Property | base | dev | prod |
|---|---|---|---|
| `server.error.include-stacktrace` | never | never | never |
| `server.error.include-message` | never | **always** | never |
| `app.security.require-https` | false | false | **true** |
| `springdoc.swagger-ui.enabled` | (attivo) | attivo | **false** |
| `springdoc.api-docs.enabled` | (attivo) | attivo | **false** |
| `server.forward-headers-strategy` | — | — | framework |
| `spring.jackson.deserialization.fail-on-unknown-properties` | true | true | true |
| `spring.data.web.pageable.max-page-size` | 100 | 100 | 100 |
| `spring.servlet.multipart.max-file-size` | 5MB | 5MB | 5MB |

`ProdSecurityChecksConfig` (`@Profile("prod")`): `@PostConstruct` che fa fallire l'avvio se
`issuer-uri` non inizia con `https://`.

---

## 8. Discrepanze rispetto al riepilogo di progetto

Elementi descritti nel riepilogo che il codice **non** implementa, o implementa
diversamente. Nessuno è stato silenziato: ognuno è ripreso nel report.

| # | Riepilogo | Codice reale |
|---|---|---|
| D-01 | «property di disattivazione dell'AudienceValidator» | **Non esiste.** L'audience è sempre validata. I test verificano l'assenza dell'interruttore costruendo la catena con e senza il validator. |
| D-02 | «scope granulari `read:viaggi` / `write:viaggi` con property di enforcement» | **Non implementati.** Nessun codice legge il claim `scope`. Il test verifica il rovescio utile: uno `scope` nel token **non** concede authority (nessun canale di escalation parallelo). |
| D-03 | «rate limiting … 429 in formato RFC 7807» | Il 429 è un JSON semplice, non un ProblemDetail (F-04). |
| D-04 | «handler per tutte le eccezioni applicative, nessuna che cade nel 500 generico» | Prima di questo lavoro 405/415/413/404/400 finivano tutte nel fallback 500 (F-02, corretto). |
| D-05 | «limiti multipart» | Configurati, ma nessun endpoint dell'API accetta upload. Il limite è difesa in profondità. |
| D-06 | «`/actuator/health` e `/info` pubblici» | Le regole ci sono, ma **`spring-boot-starter-actuator` non è fra le dipendenze**: gli endpoint non esistono e rispondono 404. |
| D-07 | «Swagger mai raggiungibile senza autenticazione» | `/swagger-ui/**` e `/v3/api-docs/**` sono `permitAll()`: in dev e nel profilo di default sono **pubblici**. In prod sono disattivati via springdoc. |
| D-08 | «CORS con allow-list» | Registrata solo su `/api/**`: le altre rotte (swagger incluso) non hanno configurazione CORS. |
| D-09 | «identità sempre dal claim sub» | Confermato ovunque **tranne** `UtenteSecurity.isSelf`, che risolve l'utente locale dal `sub` ma propaga `UtenteNonTrovatoException` (404) se l'utente non è ancora sincronizzato: un utente non sincronizzato riceve 404 invece di 403. Nessuna perdita di informazione, comportamento documentato. |
