# Report dei test di sicurezza — travelapp backend

Branch `dev/experience` · Data: **2026-08-05**

| | |
|---|---|
| Comando eseguito | `mvnw.cmd -B clean test` |
| Esito | **BUILD SUCCESS** |
| Test eseguiti | **413** |
| Failures | **0** |
| Errors | **0** |
| Skipped | **0** |
| Classi di test | 44 (di cui 26 nuove, in `security/fase*` e `security/trasversali`) |
| Durata | 46 s |
| Test esclusi | nessuno. Nessun `@Disabled`, nessun `@Ignore`, nessun test taggato `keycloak-live` |

Il filtro `<excludedGroups>keycloak-live</excludedGroups>` è configurato in Surefire, ma
**nessun test è stato marcato con quel tag**: l'intera suite gira senza un'istanza Keycloak.

---

## 1. Fase 0 — sbloccare la build

Il fallimento riportato (`commons-io:2.11.0`, *Premature end of Content-Length delimited
message body*) era un **artefatto troncato nella cache Maven locale**, non un problema di
rete o di proxy.

```
rmdir /s /q "%USERPROFILE%\.m2\repository\commons-io\commons-io\2.11.0"
(rimossi anche 2 file *.lastUpdated sotto %USERPROFILE%\.m2\repository)
mvnw.cmd -B -U -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.rto=120000 clean test-compile
```

Il jar è stato riscaricato correttamente (327 kB) al primo tentativo → **BUILD SUCCESS**.
Nessun blocco infrastrutturale.

**Baseline registrata** (`mvnw.cmd -B test` prima di scrivere qualsiasi test nuovo):
`Tests run: 38, Failures: 0, Errors: 1` — l'unico errore era
`TravelappBackendApplicationTests.contextLoads`, che falliva perché
`application.properties` richiede le variabili d'ambiente `DB_URL` / `DB_USERNAME` /
`DB_PASSWORD`. Risolto fornendo il datasource dai test (vedi §2), non modificando la
configurazione di produzione.

### Nota su PowerShell
`.\mvnw.cmd -Dmaven.wagon.http.retryHandler.count=5` viene spezzato da PowerShell in due
argomenti e Maven fallisce con *Unknown lifecycle phase*. Vanno virgolettati:
`"-Dmaven.wagon.http.retryHandler.count=5"`.

## 2. Infrastruttura di test

- **Database**: `TestDatabase` sceglie a runtime. Se il client Docker risponde usa
  **Testcontainers Postgres 16**; altrimenti ricade su **H2 in modalità PostgreSQL**.
  Su questa macchina il CLI `docker info` risponde (server 29.1.3) ma la Engine API
  restituisce `400` con payload vuoto a qualunque client non-CLI, quindi
  `DockerClientFactory.isDockerAvailable()` è falso e **la suite ha girato su H2** —
  esattamente il percorso di fallback previsto. Il ramo Testcontainers resta attivo su
  macchine in cui l'API risponde.
- **JWT, due livelli distinti**:
  - autorizzazione (matrice ruoli, BOLA) → `TestJwt`, post-processor `jwt()` con claim
    Keycloak costruiti a mano, **authority prodotte dal `KeycloakRoleConverter` di
    produzione**;
  - decoder e validator (firma, issuer, audience, scadenza) → `RsaTokenFactory` +
    `ServerJwkDiProva`: chiavi RSA generate a runtime, JWK set pubblicata da un server
    HTTP locale, e il `JwtDecoder` **di produzione** che la scarica davvero. L'unica cosa
    sostituita è l'indirizzo delle chiavi pubbliche, cioè il ruolo di Keycloak.
- **Nessun controllo indebolito**: nessun `@AutoConfigureMockMvc(addFilters = false)`,
  nessun `permitAll()` di test, nessun `csrf().disable()` aggiuntivo, nessun
  `@WithMockUser` al posto del JWT, nessuna asserzione su `is4xxClientError()`.
- **Nessuna credenziale reale**: chiavi generate nella JVM di test, nessun token vero.
- **Rinunce**: ArchUnit non è fra le dipendenze e non è stato aggiunto; il controllo
  strutturale sui controller è realizzato con reflection e lo scanner del classpath di
  Spring (`NessunaEntitaJpaNeiControllerTest`), con un test che verifica che il
  rilevatore riconosca davvero un'entità anche dentro i generici. JaCoCo non era
  configurato e non è stato aggiunto.

---

## 3. Matrice dei controlli verificati

OWASP API Security Top 10 (2023). Tutti gli esiti sono **PASS** sulla run finale.

### Fase 1 — Autorizzazione

| Controllo | Classe::metodo | OWASP | Esito |
|---|---|---|---|
| Ogni rotta `/api/**` senza token → 401 (27 rotte) | `DenyByDefaultSecurityTest::ogniRottaApiRichiedeUnTokenERispondeCon401` | API2 | PASS |
| Rotte non mappate negate agli anonimi | `DenyByDefaultSecurityTest::leRotteNonMappateSonoNegateAnchePerGliAnonimi` | API5 | PASS |
| `denyAll()` vale anche per un ADMIN autenticato | `DenyByDefaultSecurityTest::unaRottaNonMappataFuoriDaApiEnegataAncheConTokenValido` | API5 | PASS |
| Path traversal rifiutato dallo StrictHttpFirewall | `DenyByDefaultSecurityTest::ilPathTraversalNellaUrlVieneRifiutatoPrimaDiArrivareAlControllore` | API8 | PASS |
| Token malformato / schema non Bearer → 401 | `DenyByDefaultSecurityTest::unTokenMalformatoNonAutentica`, `::loSchemaDiAutorizzazioneNonBearerNonAutentica` | API2 | PASS |
| Matrice endpoint × ruolo (22 rotte × 4 identità) | `MatriceRuoliSecurityTest::ogniRuoloRispettaLaMatrice`, `::anonimoSempreRespintoCon401` | API5 | PASS |
| Utente senza ruoli applicativi (stato attuale del realm) → 403 | `MatriceRuoliSecurityTest::unUtenteAutenticatoSenzaRuoliApplicativiNonAccedeAlleRotteRiservate` | API5 | PASS |
| Ruoli client ≡ ruoli realm | `MatriceRuoliSecurityTest::iRuoliClientDiKeycloakSonoEquivalentiAiRuoliRealm` | API5 | PASS |
| Ruoli di un altro client non concedono nulla | `MatriceRuoliSecurityTest::unRuoloDiUnAltroClientNonConcedeAlcunPrivilegio` | API5 | PASS |
| Converter fail-closed: claim assente/null/tipo errato/lista vuota | `KeycloakRoleConverterFailClosedTest` (7 metodi) | API5 | PASS |
| `scope` non diventa mai una authority | `KeycloakRoleConverterFailClosedTest::ilClaimScopeNonDiventaMaiUnaAuthority` | API5 | PASS |
| Claim arbitrari (`roles`, `authorities`, `groups`) ignorati | `KeycloakRoleConverterFailClosedTest::unClaimArbitrarioDelTokenNonDiventaUnRuolo` | API5 | PASS |
| Audience corretta / `account` / lista / assente | `ValidazioneTokenSecurityTest` (4 metodi) | API2 | PASS |
| Senza AudienceValidator lo stesso token passerebbe | `ValidazioneTokenSecurityTest::senzaAudienceValidatorLoStessoTokenPasserebbe` | API2 | PASS |
| Issuer diverso (`localhost:8090` vs `travelapp-keycloak:8080`) → rifiutato | `ValidazioneTokenSecurityTest::rifiutaIlTokenConIssuerDiverso` | API2 | PASS |
| Token scaduto / firma errata / `alg=none` | `ValidazioneTokenSecurityTest` (3 metodi) | API2 | PASS |
| I validator collaudati sono quelli montati sul bean reale | `ConfigurazioneJwtDecoderSecurityTest::ilDecoderDellApplicazioneMontaSiaIlValidatorDiIssuerSiaQuelloDiAudience` | API2 | PASS |
| Token firmati veri sull'intera catena di filtri | `TokenRealeSullaCatenaSecurityTest` (8 metodi) | API2 | PASS |
| BOLA prenotazioni: lettura, pagamento, annullamento altrui | `BolaPrenotazioniSecurityTest` (11 metodi) | **API1** | PASS |
| Id altrui e id inesistente indistinguibili | `BolaPrenotazioniSecurityTest::unIdAltruiEUnIdInesistenteSonoIndistinguibili` | API1 | PASS |
| `GET /api/prenotazioni/utente/{id}` altrui → 403 | `BolaPrenotazioniSecurityTest::aNonPuoElencareLePrenotazioniDiB` | API1 | PASS |
| Escalation via payload → 400, owner verificato a DB | `BolaPrenotazioniSecurityTest::nonSiPuoIntestareUnaPrenotazioneAUnAltroUtenteDalPayload`, `::laPrenotazioneCreataRisultaIntestataAllUtenteDelToken` | **API3** | PASS |
| Ownership dal `sub`, non dall'username | `BolaPrenotazioniSecurityTest::lIdentitaVieneDalSubNonDallUsernameDelToken` | API1 | PASS |
| BOLA utenti: lettura/modifica/cancellazione altrui → 403 | `BolaUtentiSecurityTest` (11 metodi) | API1 | PASS |
| Cambio di ruolo via PUT rifiutato | `BolaUtentiSecurityTest::nonSiPuoCambiareRuoloDaSeStessiTramitePut` | API3 | PASS |
| BOLA catalogo: itinerari e attività di un altro organizzatore | `BolaContenutiSecurityTest` (11 metodi) | API1 | PASS |
| Recensione: solo l'autore cancella; niente recensioni su prenotazioni altrui | `BolaContenutiSecurityTest::soloLAutoreCancellaLaPropriaRecensione`, `::nonSiPuoRecensireLaPrenotazioneDiUnAltroUtente` | API1 | PASS |
| ADMIN accede alle risorse altrui dove previsto | `BolaPrenotazioniSecurityTest::lAdminAccedeAllePrenotazioniAltrui`, `BolaContenutiSecurityTest::lAdminCancellaQualsiasiItinerario` | API5 | PASS |

### Fase 2 — Input/output

| Controllo | Classe::metodo | OWASP | Esito |
|---|---|---|---|
| Validazione DTO itinerario (8 casi) | `ValidazioneInputSecurityTest::iPayloadDiItinerarioNonValidiVengonoRifiutatiCon400` | API8 | PASS |
| Validazione DTO prenotazione (5 casi) / recensione (4 casi) | `ValidazioneInputSecurityTest` | API8 | PASS |
| `@Size` superato, enum non valido, email malformata | `ValidazioneInputSecurityTest` (3 metodi) | API8 | PASS |
| JSON malformato / tipo errato / body vuoto / array | `ValidazioneInputSecurityTest` (4 metodi) | API8 | PASS |
| ProblemDetail elenca i campi senza esporre classi | `ValidazioneInputSecurityTest::ilProblemDetailElencaICampiInErroreSenzaEsporreClassiOPackage` | API8 | PASS |
| Mass assignment su itinerario (7 campi) e prenotazione (7 campi) | `MassAssignmentSecurityTest` (parametrizzati) | **API3** | PASS |
| Prezzo non forzabile dal client | `MassAssignmentSecurityTest::nonSiPuoForzareIlPrezzoDiUnaPrenotazione` | API3 | PASS |
| Campi di audit non accettati (6 campi) | `MassAssignmentSecurityTest::iCampiDiAuditNonSonoAccettatiInAggiornamento` | API3 | PASS |
| Nessuna entità JPA nelle firme dei controller (generici inclusi) | `NessunaEntitaJpaNeiControllerTest` (5 metodi) | API3 | PASS |
| Nessun dato sensibile nel JSON di risposta | `NessunDatoSensibileNelleRisposteTest` (7 metodi) | **API3** | PASS |
| XSS / template / SQLi / traversal / byte nulli (12 payload) | `IniezioneEXssSecurityTest::iPayloadOstiliNonRomponoLApiEIlDatoRestaIntegro` | API8 | PASS |
| SQLi nel commento: dato salvato letteralmente, tabelle intatte | `IniezioneEXssSecurityTest::unaSqlInjectionNelCommentoDiUnaRecensioneNonCancellaLaTabella` | API8 | PASS |
| SQLi in path param / `sort` / `size` non eseguita | `IniezioneEXssSecurityTest` (3 metodi) | API8 | PASS |
| Mai `text/html`, JSON sempre ben formato | `IniezioneEXssSecurityTest::laRispostaJsonNonEMaiServitaComeHtml`, `::ilCampoTestualeConTagVieneRestituitoCodificatoInJson` | API8 | PASS |

### Fase 3 — Protezione delle risorse

| Controllo | Classe::metodo | OWASP | Esito |
|---|---|---|---|
| 429 oltre la capienza, con `Retry-After` e `X-RateLimit-*` | `RateLimitSecurityTest::oltreLaCapienzaLaRichiestaSuccessivaRiceve429` | **API4** | PASS |
| Header di quota coerenti richiesta dopo richiesta | `RateLimitSecurityTest::gliHeaderDiQuotaSonoCoerentiRichiestaDopoRichiesta` | API4 | PASS |
| Bucket isolati per utente e per IP | `RateLimitSecurityTest::iBucketSonoIsolatiPerUtente`, `::ilRateLimitAnonimoDistingueGliIndirizziIp` | API4 | PASS |
| Chiave = `sub`, non username | `RateLimitSecurityTest::laChiaveDelBucketEIlSubDelTokenNonLoUsername` | API4 | PASS |
| `X-Forwarded-For` falsificato non aggira il limite | `RateLimitSecurityTest::xForwardedForNonPermetteDiAggirareIlLimiteAnonimo` | API4 | PASS |
| Header di sicurezza e assenza di leak sul 429 | `RateLimitSecurityTest::il429PortaConSeGliHeaderDiSicurezza`, `::ilCorpoDel429NonEspoDettagliInterni` | API4 | PASS |
| `size` oltre il massimo clampato a 100 (4 valori, 4 endpoint) | `PaginazioneSecurityTest` (5 metodi) | API4 | PASS |
| Default page size, valori negativi, pagina fuori range | `PaginazioneSecurityTest` (4 metodi) | API4 | PASS |
| Ordinamento su campo inesistente → 400 | `IniezioneEXssSecurityTest::unOrdinamentoSuUnCampoInesistenteVieneRifiutatoCon400` | API4 | PASS |
| Upload oltre il limite → 413 ProblemDetail (Tomcat reale) | `UploadLimiteRealeSecurityTest::unUploadOltreIlLimiteVieneRifiutatoCon413InFormatoProblemDetail` | API4 | PASS |
| L'upload oltre il limite richiede comunque autenticazione | `UploadLimiteRealeSecurityTest::unUploadOltreIlLimiteRichiedeComunqueLAutenticazione` | API2 | PASS |
| Limiti multipart / paginazione / timeout applicati al contesto | `LimitiRisorseSecurityTest` (5 metodi) | API4 | PASS |

### Fase 4 — Gestione degli errori

| Controllo | Classe::metodo | OWASP | Esito |
|---|---|---|---|
| RFC 7807 completo su 400/401/403/404/405/409/415 | `FormatoErroriSecurityTest` (11 metodi) | API8 | PASS |
| `Content-Type: application/problem+json` | `FormatoErroriSecurityTest::erroreConforme` (helper su ogni caso) | API8 | PASS |
| Conflitto di integrità senza nome del vincolo | `FormatoErroriSecurityTest::ilConflittoDiIntegritaNonEsponeIlVincoloViolato` | API8 | PASS |
| Leak check su ogni errore (12 richieste diverse) | `FormatoErroriSecurityTest::tutteLeRisposteDiErroreDellaSuitePassanoIlControlloAntiLeak` | API8 | PASS |
| Il 500 non cambia in base all'input (nessun oracolo) | `FormatoErroriSecurityTest::ilFallbackA500NonCambiaComportamentoInBaseAllInput` | API8 | PASS |
| traceId presente, unico, coerente header/body, propagabile | `TraceIdEProfiliErroreTest` (4 metodi) | API9 | PASS |
| traceId ritrovabile nei log della stessa richiesta | `TraceIdEProfiliErroreTest::ilTraceIdDellaRispostaSiRitrovaNeiLogDellaStessaRichiesta` | API9 | PASS |
| `include-stacktrace`/`include-message` = never | `TraceIdEProfiliErroreTest::ilProfiloDiBaseNonEspoStackTraceNeMessaggiGrezzi` | API8 | PASS |

Il controllo anti-leak (`NessunLeak`) è applicato a **ogni** risposta di errore prodotta
dalla suite, non a un caso singolo: cerca `exception`, `at com.`, `org.springframework`,
`org.hibernate`, `jakarta.persistence`, `caused by`, `constraint`, `jdbc`, `psql`,
frammenti SQL, `C:\`, `/home/`, `/usr/`, `.java:`.

### Fase 5 — Audit logging

| Controllo | Classe::metodo | OWASP | Esito |
|---|---|---|---|
| `creatoDa` = claim `sub`, `creatoIl` valorizzato | `JpaAuditingSecurityTest::allaCreazioneCreatoDaEIlSubDelTokenECreatoIlEValorizzato` | **API9** | PASS |
| `creatoDa`/`creatoIl` inviati dal client → rifiutati | `JpaAuditingSecurityTest::unCreatoDaInviatoDalClientVieneRifiutato` | API3 | PASS |
| Update cambia solo i campi di modifica | `JpaAuditingSecurityTest::suUnAggiornamentoCambiaSoloIlModificatoDa` | API9 | PASS |
| Audit non dipende dall'username del token | `JpaAuditingSecurityTest::lIdentitaDiAuditNonDipendeDallUsernameDelToken` | API9 | PASS |
| Eventi su create/update/delete/paga/annulla | `EventiDiAuditSecurityTest` (3 metodi) | API9 | PASS |
| Ogni evento ha subject, username, timestamp UTC, azione, risorsa, esito, ip, traceId | `EventiDiAuditSecurityTest::ogniEventoContieneTuttiICampiRichiesti` | API9 | PASS |
| Un 403 lascia traccia quanto un successo | `EventiDiAuditSecurityTest::unAccessoNegatoLasciaTracciaQuantoUnoRiuscito`, `::unTentativoDiAccessoAUnaRisorsaAltruiLasciaTraccia` | API9 | PASS |
| JSON valido su una sola riga | `EventiDiAuditSecurityTest::ogniEventoDiAuditEJsonValidoSuUnaSolaRiga` | API9 | PASS |
| `additivity=false`: nessun duplicato sul root | `EventiDiAuditSecurityTest::gliEventiDiAuditNonFinisconoAncheNelLogRoot` | API9 | PASS |
| Rollback: nessun evento di creazione fantasma | `EventiDiAuditSecurityTest::unaOperazioneFallitaInRollbackNonLasciaLaRisorsaMaLEventoDiErroreSi` | API9 | PASS |
| **Token mai nei log / audit / `logs/audit.log`** | `RedazioneSegretiNeiLogSecurityTest::ilTokenBearerNonCompareMaiNeiLogNeNelFileDiAudit` | API9 | PASS |
| Nemmeno un frammento del token (payload, firma, `eyJ`) | `RedazioneSegretiNeiLogSecurityTest::unaPortionDelTokenNonCompareNeiLog`, `::ilFileDiAuditNonContieneMaiUnTokenJwtInQualsiasiForma` | API9 | PASS |
| Password nel payload mai loggata | `RedazioneSegretiNeiLogSecurityTest::unaPasswordNelPayloadNonCompareNeiLog` | API9 | PASS |
| Autenticazione fallita: token non trascritto | `RedazioneSegretiNeiLogSecurityTest::unAutenticazioneFallitaNonLoggaIlTokenPresentato` | API9 | PASS |
| Header `Authorization` mai trascritto | `RedazioneSegretiNeiLogSecurityTest::lHeaderAuthorizationNonVieneMaiTrascritto` | API9 | PASS |

### Fase 6 — Rete e configurazione

| Controllo | Classe::metodo | OWASP | Esito |
|---|---|---|---|
| Header di sicurezza su 200 e su 400/401/403/404 | `HeaderDiSicurezzaSecurityTest` (5 metodi) | API8 | PASS |
| CSP senza wildcard | `HeaderDiSicurezzaSecurityTest::laCspVietaLaFramificazioneEGliOggettiEsterni` | API8 | PASS |
| HSTS assente su HTTP, presente su HTTPS | `HeaderDiSicurezzaSecurityTest::suHttpNonVieneEmessoHstsInSviluppo`, `::suHttpsVieneEmessoHstsConMaxAgeESubdomini` | API8 | PASS |
| Preflight da origine in allow-list | `CorsSecurityTest::ilPreflightDaUnOrigineInAllowListRiceveGliHeaderCorretti` | API8 | PASS |
| 5 origini non ammesse (inclusi quasi-omonimi e `null`) → nessun header CORS | `CorsSecurityTest::unOrigineFuoriAllowListNonRiceveAlcunHeaderCors` | API8 | PASS |
| Mai wildcard, mai `allowCredentials=true` | `CorsSecurityTest` (2 metodi) | API8 | PASS |
| Metodi e header limitati ai dichiarati | `CorsSecurityTest` (3 metodi) | API8 | PASS |
| Nessun cookie, nessuna sessione HTTP | `StatelessECsrfSecurityTest` (3 metodi) | API8 | PASS |
| Un JSESSIONID non autentica | `StatelessECsrfSecurityTest::unCookieDiSessioneFornitoDalClientNonAutentica` | API2 | PASS |
| Nessuno stato condiviso fra richieste | `StatelessECsrfSecurityTest::dueRichiesteConLoStessoClientNonCondividonoStato` | API8 | PASS |
| Assenza di CSRF non sfruttabile (nessun flusso a cookie) | `StatelessECsrfSecurityTest::unaScritturaSenzaAlcunaCredenzialeVieneRespinta`, `::unaRichiestaCrossOriginConCookieMaSenzaBearerNonPassa` | API8 | PASS |
| Fail-fast su issuer non HTTPS in prod (bean + ApplicationContext) | `ProfiliEEsposizioneSecurityTest` (4 metodi) | API8 | PASS |
| Swagger/OpenAPI disattivati in prod | `ProfiliEEsposizioneSecurityTest::inProduzioneSwaggerEOpenApiSonoDisattivati` | API8 | PASS |
| Actuator riservato negato a non-ADMIN e anonimi | `ProfiliEEsposizioneSecurityTest` (2 metodi) | API5 | PASS |
| Contesto avviabile con default/dev/test/prod | `AvvioConTuttiIProfiliTest::ilContestoSiAvviaConOgniProfilo` | — | PASS |
| Bean di sicurezza presenti in ogni profilo | `AvvioConTuttiIProfiliTest::iBeanDiSicurezzaEsistonoInOgniProfilo` | — | PASS |
| Regressione funzionale sui flussi principali | `FlussiFunzionaliSmokeTest` (8 metodi) | — | PASS |
| Nessun segreto committato | `NessunSegretoCommittatoTest` (5 metodi) | API8 | PASS |

---

## 4. Vulnerabilità trovate

### F-01 — `AudienceValidator` va in crash sui token senza claim `aud` — **MEDIA** — CORRETTA

**Descrizione.** Un JWT privo del claim `aud` ha `jwt.getAudience() == null`, non una lista
vuota. `AudienceValidator.validate()` invocava `contains()` direttamente su quel valore e
sollevava `NullPointerException`.

**Perché è un problema di sicurezza.** `NullPointerException` non è una
`AuthenticationException`: sfugge alla gestione del `BearerTokenAuthenticationFilter` e
risale l'intera catena di filtri. Un token che doveva essere **rifiutato** diventava un
**errore interno**. Conseguenze: percorso di errore non gestito raggiungibile da un
chiamante non autenticato (stack trace ERROR a ogni richiesta → amplificazione dei log), e
un controllo di sicurezza che va in crash invece di negare in modo pulito — la classica
premessa di un fail-open dopo un refactoring.

**Riproduzione.** Token firmato correttamente dall'IdP ma senza `aud`
(configurazione possibile in Keycloak quando il client non ha audience mapper — cioè lo
stato attuale del realm):

```
GET /api/itinerari
Authorization: Bearer <token firmato, senza claim aud>
→ atteso 401 · osservato: NullPointerException non gestita
```

**Correzione** (`config/AudienceValidator.java`, commit `6362132`):

```java
List<String> audience = jwt.getAudience();
if (audience != null && audience.contains(expectedAudience)) { ... }
```

**Perché il test preesistente non lo copriva.** `AudienceValidatorTest.rifiutaTokenSenzaAudience`
costruiva il token con `audience(List.of())`, cioè una **lista vuota**, non un claim
assente: passava senza esercitare il caso reale.

**Regressione.** `ValidazioneTokenSecurityTest::rifiutaIlTokenSenzaAudience` (livello
decoder) e `TokenRealeSullaCatenaSecurityTest::unTokenSenzaClaimAudVieneRespintoCon401`
(token firmato, catena reale → 401).

---

### F-02 — Gli errori 4xx del client venivano restituiti come 500 — **BASSA/MEDIA** — CORRETTA

**Descrizione.** `@ExceptionHandler(Exception.class)` intercettava anche le eccezioni che
Spring solleva per gli errori del chiamante, perché `ExceptionHandlerExceptionResolver` ha
precedenza su `DefaultHandlerExceptionResolver`. Misurato empiricamente:

| Richiesta | Prima | Dopo |
|---|---|---|
| `PATCH /api/itinerari/1` (verbo non ammesso) | 500 | **405** |
| `POST /api/itinerari` con `Content-Type: text/plain` | 500 | **415** |
| `GET /api/nonesiste` con token valido | 500 | **404** |
| Upload oltre il limite multipart | 500 | **413** |
| `GET /api/prenotazioni/abc` (tipo errato) | 500 | **400** |
| `GET /api/itinerari?sort=campoInesistente,asc` | 500 | **400** |

**Perché è un problema di sicurezza.** Un chiamante può generare a volontà stack trace di
livello ERROR (amplificazione dei log: rumore che copre gli eventi veri e consuma spazio), e
qualunque allarme basato sul tasso di 5xx diventa inutilizzabile perché i 5xx sono in gran
parte errori del client. Il limite multipart in particolare aveva il controllo attivo ma il
percorso d'errore rotto.

**Correzione** (`exception/GlobalExceptionHandler.java`, commit `4ef1a91`): handler
espliciti e **additivi** per `HttpRequestMethodNotSupportedException`,
`HttpMediaTypeNotSupportedException`, `HttpMediaTypeNotAcceptableException`,
`NoResourceFoundException`, `MaxUploadSizeExceededException`,
`MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`,
`PropertyReferenceException`. Riusano lo stesso costruttore di `ProblemDetail`, quindi
mantengono formato RFC 7807, `traceId` e assenza di dettagli interni. Nessun handler
esistente è stato toccato.

**Scelta conservativa documentata.** `InvalidDataAccessApiUsageException` **non** è stata
mappata: copre anche errori di programmazione lato server e mapparla su 400 nasconderebbe
bug veri. Resta quindi il caso F-03.

---

### F-03 — Espressione di ordinamento non sicura → 500 — **INFORMATIVA** — NON corretta

`GET /api/itinerari?sort=titolo; DROP TABLE utenti` produce 500.

**L'iniezione non viene eseguita**: Spring Data JPA rifiuta le espressioni di ordinamento
che non sono riferimenti a proprietà (`QueryUtils.checkSortExpression`), quindi la stringa
non raggiunge il database — verificato asserendo che le tabelle esistono ancora. Il body è
il ProblemDetail generico e supera il controllo anti-leak. Resta la sola imprecisione dello
status. Non corretta per la ragione spiegata in F-02.

Test: `IniezioneEXssSecurityTest::unaSqlInjectionNelParametroDiOrdinamentoNonVieneEseguita`.

---

### F-04 — Il 429 non è in formato RFC 7807 — **INFORMATIVA** — NON corretta

`RateLimitFilter` scrive direttamente `{"status":429,"errore":"..."}` con
`Content-Type: application/json`, mentre tutte le altre risposte di errore sono
`application/problem+json` con `type`, `title`, `instance` e `traceId`. Il filtro gira
prima del `DispatcherServlet`, quindi non passa dal `@RestControllerAdvice`.

Nessuna informazione sensibile trapela (verificato con il controllo anti-leak) e gli header
di sicurezza sono presenti. È un'incoerenza di formato, non una vulnerabilità: correggerla
richiederebbe di iniettare un serializzatore nel filtro, cioè una modifica di design fuori
dal perimetro di questo lavoro.

Test: `RateLimitSecurityTest::ilCorpoDel429NonEspoDettagliInterni`.

---

### F-05 — Segreti reali committati nella collection Postman — **ALTA** — CORRETTA (rotazione ancora necessaria)

**Descrizione.** `NessunSegretoCommittatoTest` ha trovato in
`postman/collections/TRAVELAPP API.postman_collection.json` (file **tracciato da git**):

- il **`client_secret` del client Keycloak `travelapp-backend`** (32 caratteri) — credenziale
  che **non scade**;
- la password di un utente di prova (`test123`);
- 8 access token Keycloak completi, con realm, `sub`, email e session id dell'utente
  (scaduti il 2026-05-18, ma comunque informativi sulla topologia interna).

**Perché è grave.** Con il client secret un attaccante può ottenere token da Keycloak
impersonando il client confidenziale, con i privilegi che la configurazione del client
concede. È il segreto più sensibile del sistema, ed era in chiaro nel repository.

**Correzione** (commit `e78e538`): valori sostituiti con variabili di collection
(`{{client_secret}}`, `{{password}}`, `{{access_token}}`), dichiarate vuote; la collection
resta usabile valorizzandole in un environment locale, che `.gitignore` ora esclude.

> **Azione ancora necessaria, fuori dalla portata del codice.** La rimozione dal working
> tree non basta: i valori restano nella **storia di git** e vanno considerati compromessi.
> 1. rigenerare il client secret di `travelapp-backend` in Keycloak;
> 2. cambiare la password dell'utente di prova;
> 3. valutare la riscrittura della storia (`git filter-repo`); se il repository è già stato
>    condiviso, considerare il segreto definitivamente esposto.

---

### F-06 — `POST /api/preferiti` fallisce con 500 al primo preferito — **FUNZIONALE** — CORRETTA

`Preferito.itinerario` era una `List` non inizializzata; `PreferitoService.addPreferito`
chiamava `prefe.getItinerario().add(itin)` su un `Preferito` appena creato →
`NullPointerException` → 500. La funzionalità "preferiti" era quindi **inutilizzabile per
ogni nuovo utente**: la lista non veniva mai creata, quindi nemmeno lettura e rimozione
potevano funzionare.

Non è un problema di sicurezza (nessun leak, nessun bypass): inizialmente era stato solo
segnalato, in ossequio alla regola che limita le modifiche di produzione ai bug di
sicurezza. **Corretto successivamente su richiesta esplicita** (commit `1c2771a`).

**Correzione** (`experience/models/Preferito.java`): campo inizializzato a
`new ArrayList<>()`. Scelta l'entità invece del service perché una collection JPA non deve
mai essere `null`: così sono coperti tutti e tre i punti che la dereferenziano
(`addPreferito`, `removePreferito`, `getPreferiti`) e non solo il call site in errore.

**Regressione**: `PreferitiFlussoTest` (6 metodi) — primo inserimento, secondo inserimento
sulla lista esistente, lettura, rimozione, separazione fra utenti diversi, nessuna lista
fantasma quando l'itinerario non esiste. Verificato che tornino rossi con
`NullPointerException` annullando la correzione.

**Nota sul profilo di test.** Nel farlo è emerso che `application-test.yml` impostava
`spring.jpa.open-in-view=false`, mentre in produzione vale il default di Spring Boot
(attivo). I test giravano quindi su una configurazione inesistente e `GET /api/preferiti`
falliva per `LazyInitializationException` che nell'applicazione reale non si verifica.
La riga è stata rimossa: il profilo di test ora rispecchia la produzione.

---

### F-07 — `POST /api/utenti/me` crea utenti con email vuota — **BASSA** — NON corretta

`UtenteService.sincronizzaUtente` usa `""` come default quando il token non porta i claim
`email` / `given_name` / `family_name`. La colonna `email` è `unique NOT NULL`: **il secondo
utente senza claim email fallisce con violazione di vincolo** (409). Poiché il realm oggi
non rilascia lo scope `email` in tutti i flussi, la registrazione self-service può bloccarsi
dopo il primo utente. Segnalato, non corretto (non è una vulnerabilità).

---

## 5. Discrepanze riepilogo ↔ codice

Riportate per esteso in `docs/security-inventory.md` §8. In sintesi:

| # | Sintesi |
|---|---|
| D-01 | La «property di disattivazione dell'AudienceValidator» **non esiste**: l'audience è sempre validata. |
| D-02 | Gli **scope granulari** `read:viaggi` / `write:viaggi` **non sono implementati**: nessun codice legge il claim `scope`. |
| D-03 | Il 429 **non** è in formato RFC 7807 (F-04). |
| D-04 | «nessuna eccezione cade nel 500 generico» non era vero prima di F-02. |
| D-05 | I limiti multipart sono configurati ma **non esiste alcun endpoint di upload**. |
| D-06 | `spring-boot-starter-actuator` **non è fra le dipendenze**: `/actuator/health` e `/info` rispondono 404, non 200. |
| D-07 | Swagger e `/v3/api-docs` sono `permitAll()`: **pubblici** in dev e nel profilo di default. |
| D-08 | La configurazione CORS è registrata **solo su `/api/**`**. |
| D-09 | Un utente non ancora sincronizzato riceve **404 invece di 403** su `/api/utenti/{id}` (`UtenteSecurity.isSelf` propaga `UtenteNonTrovatoException`). |

Come richiesto, i test sono scritti su ciò che il codice fa davvero, e ogni discrepanza è
segnalata qui invece di essere assorbita da un'asserzione permissiva.

---

## 6. Verifica di efficacia dei test

Per i cinque controlli più critici il controllo è stato **neutralizzato**, la suite
rieseguita, e il controllo **ripristinato**. Se un test fosse rimasto verde sarebbe stato
riscritto: non è successo.

| # | Controllo | Neutralizzazione applicata | Test diventati rossi | Esito |
|---|---|---|---|---|
| 1 | Deny-by-default | `SecurityConfig`: `/api/**` → `permitAll()`, `anyRequest()` → `permitAll()` | **29 / 38** in `DenyByDefaultSecurityTest` | ROSSO ✔ |
| 2 | Ownership prenotazioni | `PrenotazioneService`: `findByIdAndViaggiatoreId(id, viaggiatoreId)` → `findById(id)` | **5** in `BolaPrenotazioniSecurityTest` + **2** in `PrenotazioneServiceOwnershipTest` | ROSSO ✔ |
| 3 | AudienceValidator | `SecurityConfig`: `AudienceValidator` rimosso dalla catena di validator | **3** in `TokenRealeSullaCatenaSecurityTest` + **1** in `ConfigurazioneJwtDecoderSecurityTest` | ROSSO ✔ |
| 4 | Rate limiting | `RateLimitFilter`: pass-through incondizionato dopo il consumo del gettone | **8 / 9** in `RateLimitSecurityTest` + **1** in `RateLimitFilterTest` | ROSSO ✔ |
| 5 | Redazione dei segreti | `AuditLogger`: aggiunto `evento.put("tokenValue", jwt.getTokenValue())` | **4 / 7** in `RedazioneSegretiNeiLogSecurityTest` + **1** in `AuditLoggerTest` | ROSSO ✔ |

**Ripristino verificato.** Dopo ogni neutralizzazione: `git checkout -- <file>`.
`git status --porcelain` al termine non riporta alcuna modifica non voluta a `src/main`, e
`git diff HEAD -- src/main` è vuoto. L'unica voce emersa era la cartella `logs/` generata a
runtime dall'appender di audit, ora in `.gitignore`.

Nota: neutralizzare un controllo ha reso rossi anche alcuni test **preesistenti**
(`PrenotazioneServiceOwnershipTest`, `RateLimitFilterTest`, `AuditLoggerTest`), il che
conferma che anche quelli erano test di sicurezza veri e non tautologie.

---

## 7. Non coperto da test automatici

Resta verificabile solo end-to-end **dopo la configurazione di Keycloak**. Nessuno di questi
punti è necessario perché la suite passi.

| Area | Perché non è automatizzabile ora |
|---|---|
| Audience mapper del realm | Serve che Keycloak emetta `aud=travelapp-backend`. Oggi emette `aud=account`. La logica di validazione è già collaudata con token firmati localmente. |
| Ruoli realm/client reali | Serve creare `VIAGGIATORE`, `ORGANIZZATORE`, `ADMIN` e assegnarli. La conversione dei claim in authority è già collaudata. |
| Scope granulari | **Non implementati nel codice** (D-02): prima vanno scritti, poi configurati nel realm. |
| `KC_HOSTNAME_URL` / coerenza issuer | Il disallineamento `localhost:8090` ↔ `travelapp-keycloak:8080` è collaudato come logica, ma il valore reale emesso dal container va verificato dal vivo. |
| Web Origins del client Keycloak | Il CORS lato API è collaudato; quello lato Keycloak (pagina di login) no. |
| Rate limiting distribuito | I bucket sono in-memory: con più istanze il limite è per-istanza. Va verificato in staging con più repliche. |
| HSTS/redirect HTTPS reali | Collaudati a livello di configurazione e header; il comportamento dietro il reverse proxy TLS va verificato in staging. |

### Comandi `curl` pronti a configurazione fatta

```bash
KC=http://localhost:8090
REALM=travelapp
API=http://localhost:8081

# 1. Token per un viaggiatore (sostituire il client secret rigenerato dopo F-05)
TOKEN=$(curl -s -X POST "$KC/realms/$REALM/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=travelapp-backend \
  -d client_secret="$CLIENT_SECRET" \
  -d username=viaggiatore -d password="$PASSWORD" | jq -r .access_token)

# 2. L'audience deve essere travelapp-backend, NON account
echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq '.aud, .realm_access.roles, .scope'

# 3. Chiamata autenticata: attesi 200 e header di sicurezza
curl -i -H "Authorization: Bearer $TOKEN" "$API/api/itinerari"

# 4. Senza token: atteso 401
curl -i "$API/api/itinerari"

# 5. Token con audience sbagliata (client diverso): atteso 401
curl -i -H "Authorization: Bearer $TOKEN_ALTRO_CLIENT" "$API/api/itinerari"

# 6. Ruolo insufficiente: atteso 403
curl -i -H "Authorization: Bearer $TOKEN" "$API/api/utenti"

# 7. BOLA: prenotazione di un altro utente, atteso 404
curl -i -H "Authorization: Bearer $TOKEN" "$API/api/prenotazioni/<id-di-un-altro>"

# 8. Rate limit: la 61esima richiesta nel minuto deve dare 429
for i in $(seq 1 65); do
  curl -s -o /dev/null -w "%{http_code} " -H "Authorization: Bearer $TOKEN" "$API/api/itinerari"
done; echo

# 9. CORS da origine non ammessa: nessun Access-Control-Allow-Origin in risposta
curl -i -X OPTIONS "$API/api/itinerari" \
  -H "Origin: https://sito-malevolo.example" -H "Access-Control-Request-Method: GET"

# 10. Swagger in produzione: atteso 404
curl -i "$API/v3/api-docs"

# 11. Il log di audit non deve contenere il token
grep -c "$(echo "$TOKEN" | cut -c1-25)" logs/audit.log   # atteso: 0
```

---

## 8. Commit prodotti

| Commit | Contenuto |
|---|---|
| `8d9c612` | `test(security)`: infrastruttura di test |
| `6362132` | **`fix(security)`**: AudienceValidator fail-closed (F-01) |
| `0f9b72e` | `test(security)`: fase 1 — autorizzazione, ruoli e BOLA |
| `4ef1a91` | **`fix(security)`**: gli errori 4xx non diventano più 500 (F-02) |
| `d0472a0` | `test(security)`: fase 2 — validazione input e output |
| `5494b68` | `test(security)`: fase 3 — protezione delle risorse |
| `7485b23` | `test(security)`: fasi 4 e 5 — errori e audit |
| `e78e538` | **`fix(security)`**: rimossi i segreti dalla collection Postman (F-05) |
| `8d78674` | `test(security)`: fase 6 e test trasversali |
| `1f6df9b` | `docs(security)`: inventario e report |
| `1c2771a` | **`fix(experience)`**: lista dei preferiti inizializzata (F-06) |
