# Configurazione Keycloak richiesta (Fase 1)

Queste modifiche vanno applicate manualmente sull'istanza Keycloak (realm `travelapp`,
console admin) o aggiunte al file di import `keycloak-import/travelapp-realm.json` a cura
del team che gestisce l'ambiente. Non vengono applicate automaticamente dal codice.

## 1. Audience nel token (obbligatorio, breaking change)

Dalla Fase 1c il backend valida il claim `aud` del JWT (proprietà
`app.security.expected-audience`, default `travelapp-backend`). **Senza questa modifica
lato Keycloak, tutte le richieste falliranno con 401 `invalid_token`**, perché di default
i client Keycloak non includono automaticamente il proprio `client_id` in `aud`.

**Il mapper va sul client che *emette* i token, non sul backend.** È l'errore più facile da
fare: uno scope dedicato si applica solo ai token rilasciati per quel client, quindi un
mapper su `travelapp-backend` — che non emette più nulla (§5, e `login-android-setup.md`) —
non entra mai in gioco. Oggi i client che emettono token sono:

| Client | Mapper audience | Dove |
|---|---|---|
| `travelapp-android` | già nel file di import | `keycloak-import/travelapp-realm.json` |
| `travelapp-test` (solo sviluppo) | **da creare a mano** | console, procedura qui sotto |

Su un realm creato da zero non c'è quindi nulla da fare per `travelapp-android`. Per
`travelapp-test`, o per un client nuovo, i passi sono (console admin → realm `travelapp`):

1. Clients → *il client che emette i token* → Client scopes → `<client-id>-dedicated` → Add mapper → By configuration
2. Scegli **Audience**
3. Configura:
   - Name: `aud-travelapp-backend`
   - Included Client Audience: `travelapp-backend`
   - Add to ID token: off
   - Add to access token: **on**
4. Salva

Verifica: decodifica un access token ottenuto da quel client su jwt.io e controlla che
`aud` contenga `"travelapp-backend"`.

Il senso del claim `aud` è "per chi è questo token": impedisce che un token ottenuto per un
servizio venga rigirato a un altro servizio dello stesso realm.

## 2. Ruolo ADMIN (Fase 1a)

L'enum locale `Ruolo` ora include `ADMIN`, ma l'autorizzazione Spring Security si basa
esclusivamente sui ruoli presenti nel JWT (claim `realm_access.roles`), non sul campo
`Utente.ruolo` nel database.

Passi:

1. Realm settings → Realm roles → Create role → nome `ADMIN`
2. Users → seleziona l'utente amministratore → Role mapping → Assign role → `ADMIN`
3. (Opzionale ma consigliato) allinea anche il campo locale: dopo il primo login
   dell'admin, aggiorna manualmente `Utente.ruolo = ADMIN` per quell'utente nel database,
   così le response API (`UtenteResponseDto.ruolo`) sono coerenti con i permessi reali.

> Da quando esiste `POST /api/auth/registrazione` (vedi §5), `VIAGGIATORE` e `ORGANIZZATORE`
> vengono assegnati automaticamente dal backend. `ADMIN` resta l'unico ruolo che si assegna
> **solo** a mano da questa procedura: la registrazione self-service non può concederlo.
> Anche il campo locale `Utente.ruolo`, per gli utenti che passano dalla registrazione, è ora
> allineato al ruolo realm; resta da correggere a mano solo per gli admin.

Il mapping realm role → claim `realm_access.roles` è già presente di default nel client
scope `roles` (protocol mapper "realm roles", già configurato in
`keycloak-import/travelapp-realm.json`), quindi non serve altro.

## 3. Ruoli client (resource_access), se servono in futuro

Il converter (`KeycloakRoleConverter`) legge anche `resource_access.travelapp-backend.roles`
per eventuali ruoli specifici del client (distinti dai ruoli realm). Anche questo mapping
è già coperto dal protocol mapper "client roles" del client scope `roles`. Per usarlo:
Clients → `travelapp-backend` → Roles → Create role, poi assegnalo a un utente da
Users → Role mapping → filtra per client `travelapp-backend`.

## 4. Scope granulari (Fase 1d, facoltativo/rimandato)

Non ancora implementato lato Spring (`@PreAuthorize("hasAuthority('SCOPE_...')")`). Se in
futuro serve, creare client scope dedicati (es. `read:viaggi`, `write:viaggi`) e assegnarli
come optional/default client scope al client `travelapp-backend`.

## 5. Service account per la registrazione self-service

`POST /api/auth/registrazione` crea l'utente su Keycloak tramite l'Admin REST API. Serve un
client dedicato con service account abilitato, **distinto** da `travelapp-backend`: sono due
identità con privilegi molto diversi (una valida token, l'altra crea utenti) e devono poter
essere ruotate e revocate separatamente.

Su un realm creato da zero il client è già nell'import (`keycloak-import/travelapp-realm.json`),
insieme ai ruoli realm `VIAGGIATORE` / `ORGANIZZATORE` / `ADMIN`. Su un realm **già esistente**
l'import non viene riapplicato: va creato a mano.

1. Clients → Create client → Client ID `travelapp-registration` → Next
2. Client authentication: **On**; Authorization: Off
3. Authentication flow: togliere **tutte** le spunte tranne **Service accounts roles**
   (niente Standard flow, niente Direct access grants: questo client non deve poter
   rappresentare un utente finale) → Next → Save
4. Scheda **Service accounts roles** → Assign role → filtro **Filter by clients** →
   assegnare da `realm-management`: `manage-users` e `view-realm`
5. Scheda **Credentials** → copiare il Client secret e passarlo al backend come variabile
   d'ambiente `KEYCLOAK_ADMIN_CLIENT_SECRET` (mai nel file di properties)

Il secret nel file di import è mascherato (`**********`), come già per `travelapp-backend`:
nessun segreto reale è versionato.

Istruzioni operative e prove passo-passo: `docs/registrazione-test-postman.md`.
Scelte di progetto e motivazioni: `docs/registrazione-implementazione.md`.

## 6. Password, verifica email e brute force (da applicare a mano sull'istanza esistente)

`keycloak-import/travelapp-realm.json` è stato aggiornato, ma **il file di import non tocca
un realm già esistente**: `--import-realm` importa solo se il realm non c'è. Su un'istanza
già avviata queste impostazioni vanno replicate dalla console, altrimenti restano attive solo
dopo un `docker compose down -v` (che cancella anche utenti e sessioni).

Realm settings → scheda indicata:

| Scheda | Impostazione | Valore | Perché |
|---|---|---|---|
| Authentication → Policies → Password policy | `Minimum Length` | `12` | le regole del backend valgono solo in registrazione: una password cambiata altrove le aggirerebbe |
| " | `Digits` | `1` | allineata a `PasswordSicura` |
| " | `Not Username`, `Not Email` | — | difesa aggiuntiva, non esprimibile lato DTO |
| Security defenses → Brute force detection | `Enabled` | on | il rate limit del backend **non** copre il login: quello avviene su Keycloak e non passa dall'applicazione |
| " | `Max login failures` | `10` | il default 30 è troppo permissivo per una password |
| " | `Permanent lockout` | off | il blocco permanente trasforma un attacco in un disservizio per la vittima |
| Login | `Verify email` | on | senza, chiunque può registrarsi con l'indirizzo di un altro |
| Login | `Forgot password` | on | unica strada di recupero che non passi da un amministratore |
| Email | host/porta SMTP | `travelapp-mailpit` : `1025` | vedi sotto |

**L'SMTP non è opzionale una volta acceso `Verify email`.** Keycloak manda la mail di
verifica al primo login: senza un server SMTP raggiungibile l'utente resta bloccato su una
schermata che gli chiede di controllare una casella dove non arriverà mai nulla. Per lo
sviluppo `docker-compose.yml` include **Mailpit**, che cattura le mail invece di spedirle:
si leggono su <http://localhost:8025>. In produzione va sostituito con un SMTP vero.

Effetto collaterale voluto sulla registrazione: da ora `POST /api/auth/registrazione` crea
l'utente con `emailVerified: false`. L'account esiste, ma il primo login passa dalla verifica
dell'indirizzo. È il punto: prima l'applicazione dichiarava verificato un indirizzo che
nessuno aveva mai provato di saper leggere.

Lo stesso vale al **cambio** email: `PUT /api/utenti/{id}` con un indirizzo diverso manda a
Keycloak anche `emailVerified: false`, quindi la verifica riparte. Un indirizzo nuovo non
eredita la prova ottenuta su quello vecchio — altrimenti basterebbe verificarsi su una
casella propria e poi spostare l'account sull'email di un'altra persona. Quando l'email non
cambia il campo non viene inviato affatto, e lo stato su Keycloak resta intatto.

**La policy password del realm è più severa dei vincoli del backend** (`@PasswordSicura` non
può esprimere `notUsername` e `notEmail`): una password può superare la validazione locale e
venire respinta da Keycloak. In quel caso registrazione e cambio password rispondono `400`
`password-non-conforme`, non `503`.

## 7. Cambio password: cosa deve fare il client

`POST /api/utenti/me/password` non chiede la password attuale — verificarla lato server
richiederebbe di riattivare il password grant, cioè il flusso che il §5 ha eliminato. Al suo
posto pretende un'**autenticazione recente**: il token deve portare un claim `auth_time` non
più vecchio di `app.security.max-auth-age-seconds` (5 minuti di default).

Il client che riceve `401` con `WWW-Authenticate: Bearer error="insufficient_user_authentication"`
deve rifare il login sull'authorization endpoint con `max_age=300`, **non** limitarsi a
rinnovare il token col refresh: il refresh non aggiorna `auth_time`, quindi riproverebbe
all'infinito. Dopo il cambio, Keycloak chiude tutte le sessioni dell'utente: serve un nuovo
login, ed è voluto — altrimenti chi avesse rubato un token manterrebbe l'accesso proprio
mentre la vittima cerca di toglierglielo.

## 8. Cosa c'è nel file di import, e cosa resta da fare a mano

`keycloak-import/travelapp-realm.json` ricrea il realm su una macchina nuova, in CI o dopo un
`docker compose down -v`. Contiene i tre client applicativi, ognuno con un solo mestiere:

| Client | Mestiere | Emette token per | Flussi |
|---|---|---|---|
| `travelapp-backend` | resource server: riceve i token e li valida | nessuno | tutti spenti |
| `travelapp-android` | fa fare il login all'utente | l'utente finale | solo Standard flow, PKCE `S256` |
| `travelapp-registration` | service account per l'Admin API | sé stesso | solo service account |

Sono presidiati da `ConfigurazioneRealmTest`: se qualcuno riaccende un flusso o toglie il
mapper audience, il test diventa rosso. Le modifiche fatte in console **non** aggiornano il
file, quindi è il file a restare la fonte di verità.

Restano da fare a mano, in questo ordine, su un realm appena creato:

1. **Ruolo `ADMIN` assegnato a un utente** (§2). L'import crea il ruolo, non lo assegna.
2. **`KEYCLOAK_ADMIN_CLIENT_SECRET`**: il secret di `travelapp-registration` è mascherato nel
   file (`**********`), quindi Keycloak ne genera uno nuovo. Va letto da Clients →
   `travelapp-registration` → Credentials e passato al backend (§5).
3. **`travelapp-test`**, solo in sviluppo (§1 e `login-android-setup.md` passo 5).

> **Il punto 3 è quello che sorprende.** `travelapp-test` è deliberatamente fuori dall'import
> — un client con il password grant acceso non deve poter seguire il realm in produzione per
> distrazione. Il rovescio della medaglia è che **`docker compose down -v` lo cancella**, e
> con lui il modo di ottenere un token da curl o Postman: le due guide di collaudo lo danno
> per esistente. Se dopo un reset dell'ambiente il token endpoint risponde
> `unauthorized_client`, è questo, e si ricrea in due minuti.

### Le chiavi del realm non stanno nel file (e quelle vecchie sono da considerare bruciate)

Il file **non** contiene il blocco `components → org.keycloak.keys.KeyProvider`: Keycloak
genera chiavi nuove quando all'import non le trova, ed è il comportamento voluto. Le chiavi
sono materiale crittografico, diverso in ogni ambiente; il file descrive la configurazione.
`ConfigurazioneRealmTest.ilFileDiImportNonContieneChiaviDelRealm` impedisce che rientrino
riesportando il realm dalla console.

**Perché è importante.** Fino alla loro rimozione il file conteneva in chiaro la chiave
privata RSA con cui il realm firma i token, più i segreti HMAC e AES. Con quella chiave
chiunque legga il repository può firmarsi da sé un access token con il `sub` di un utente
qualsiasi e `realm_access.roles: ["ADMIN"]`: la firma verifica contro la chiave pubblica che
Keycloak espone sul JWKS, quindi il backend lo accetta come un token qualunque. Nessun
controllo applicativo può accorgersene — per il resource server quel token è valido — e
nemmeno il vincolo di autenticazione recente sul cambio password regge, perché anche
`auth_time` lo scrive chi forgia il token.

**Cosa fare, una volta sola.** Le chiavi restano nella storia di git, quindi vanno sostituite
ovunque siano state usate:

- **In sviluppo**: `docker compose down -v` e riavvio. Il realm riparte con chiavi nuove (e
  vuoto: vale la lista qui sopra, `travelapp-test` compreso).
- **Su un realm che non si può ricreare**: Realm settings → Keys → Providers → *Add provider*
  `rsa-generated` con priorità più alta di quello esistente, poi rimuovere il vecchio. I token
  già emessi con la chiave vecchia restano validi fino a scadenza (5 minuti).

Nota su `admin-cli`: è un client built-in di ogni realm Keycloak, pubblico e con il password
grant acceso. Finché resta così, il password grant è comunque disponibile sul realm, e la
separazione dei flussi ottenuta sui client applicativi è meno netta di quanto sembri. Non è
stato toccato perché disabilitare un built-in è una decisione a sé, che va presa sapendo
quali strumenti amministrativi smettono di funzionare.

## Riepilogo proprietà applicative coinvolte

| Property | Default | Env var override |
|---|---|---|
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://localhost:8090/realms/travelapp` | `OAUTH2_ISSUER_URI` |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `.../protocol/openid-connect/certs` | `OAUTH2_JWK_SET_URI` |
| `app.security.expected-audience` | `travelapp-backend` | `SECURITY_EXPECTED_AUDIENCE` |
| `app.security.resource-client-id` | `travelapp-backend` | `SECURITY_RESOURCE_CLIENT_ID` |
| `app.keycloak.admin.base-url` | `http://localhost:8090` | `KEYCLOAK_BASE_URL` |
| `app.keycloak.admin.realm` | `travelapp` | `KEYCLOAK_REALM` |
| `app.keycloak.admin.client-id` | `travelapp-registration` | `KEYCLOAK_ADMIN_CLIENT_ID` |
| `app.keycloak.admin.client-secret` | *(nessuno: senza, la registrazione risponde 503)* | `KEYCLOAK_ADMIN_CLIENT_SECRET` |
| `app.keycloak.admin.connect-timeout-ms` | `5000` | `KEYCLOAK_ADMIN_CONNECT_TIMEOUT_MS` |
| `app.keycloak.admin.read-timeout-ms` | `10000` | `KEYCLOAK_ADMIN_READ_TIMEOUT_MS` |
| `app.security.max-auth-age-seconds` | `300` | `SECURITY_MAX_AUTH_AGE_SECONDS` |

In produzione, `issuer-uri` deve essere sempre HTTPS (oggi `sslRequired: "external"` nel
realm consente HTTP per richieste "interne": da rivedere in Fase 6 per un deploy reale).
