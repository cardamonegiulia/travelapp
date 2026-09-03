# TravelApp

Progetto universitario composto da un **backend Spring Boot** e da
un'**app Android nativa in Kotlin/Jetpack Compose**, con autenticazione delegata a **Keycloak**
(OAuth2 / OpenID Connect, Authorization Code + PKCE).

L'applicazione permette di pubblicare e prenotare **viaggi organizzati** (itinerari con più giorni,
tappe e partenze) e **singole attività** (escursioni con sessioni a data e ora), gestendo l'intero
ciclo: catalogo → prenotazione → pagamento → viaggio concluso → recensione.

membri del gruppo e i loro contributi:

- Alessandro Curcio: 
- occupato del lato experience:
  - fatta la sezione degli itinerari preferiti, con le liste private e quelle condivise.
  - messa l'opzione di poter lasciare commenti e recensioni sui viaggi fatti.
  - fatto il caricamento delle immagini, dall'upload al salvataggio su disco.
  - curato il lato sicurezza: validazione dei JWT, rate limiting, HTTPS e controllo sul tipo delle immagini caricate.
  - lato frontend fatto un po' di tutto: soprattutto la sezione preferiti, ma anche aggiustamenti alle parti dei colleghi in explore, booking, profile e lato organizzatore.


- Alessandro Giancarelli:
  - Per quanto riguarda il mio contributo al progetto, mi sono occupato principalmente della parte relativa alle prenotazioni e ai pagamenti.
Backend:
Ho lavorato sulla gestione della prenotazione di itinerari e attività singole, sulla gestione del numero di partecipanti, degli eventuali extra, del calcolo del prezzo totale e dell’aggiornamento dei posti disponibili. Mi sono occupato inoltre della gestione degli stati della prenotazione e del pagamento, compresa la possibilità di completare in un secondo momento un pagamento rimasto in attesa e di annullare una prenotazione, con il relativo ripristino dei posti, aggiunto anche una scadenza di 15 minuti per i pagamenti non completati: una volta superato il tempo disponibile, la prenotazione viene annullata automaticamente, il pagamento passa allo stato annullato e i posti prenotati vengono nuovamente resi disponibili. Il pagamento, per alcune limitazioni legate al progetto, è stato simulato e non collegato a un vero servizio di pagamento esterno.
Frontend:
Lato applicazione mi sono occupato delle schermate relative al flusso di prenotazione e pagamento, della selezione dei partecipanti, degli extra e dei diversi metodi di pagamento simulati. Ho inoltre gestito la visualizzazione delle prenotazioni effettuate, la possibilità di completare un pagamento in attesa e il countdown relativo alla scadenza del pagamento.
Ho anche sistemato alcuni aspetti della navigazione tra le schermate e fatto varie verifiche e integrazioni con il lavoro degli altri membri del gruppo quando necessario.


- Giulia Cardamone:
  - Nell'ambito del progetto ho contribuito principalmente alla gestione degli utenti e all'autenticazione, occupandomi sia della parte backend che dell'applicazione Android.
Per quanto riguarda il backend, ho collaborato alla realizzazione delle funzionalità di base relative all'utente, quali la creazione, la lettura, la modifica e la cancellazione dei dati, effettuando inoltre i relativi test tramite Postman. Ho partecipato all'integrazione con Keycloak per la gestione del login e della sicurezza, occupandomi anche della sincronizzazione dei dati tra Keycloak e il database. Ho contribuito inoltre alla configurazione dell'ambiente Docker, per semplificare l'avvio del progetto, e alla stesura della documentazione delle API tramite Swagger.
Per quanto riguarda l'applicazione Android, mi sono occupata delle schermate di login, registrazione con scelta del ruolo, cambio password e cambio tema, oltre alla risoluzione di alcune problematiche relative al login, al logout e al reindirizzamento tra le schermate. Ho curato infine una configurazione che, in fase di test su dispositivo fisico via USB, collega automaticamente le porte del computer a quelle del telefono, rendendo backend, Keycloak e servizio email raggiungibili senza operazioni manuali. 


- Alessia Sica:
  - Mi sono occupata dello sviluppo del modulo catalogo e della gestione dei viaggi, sia lato backend che frontend:
    Backend: Ho gestito la parte relativa a itinerari e singole attività, creando la struttura del database, la logica di gestione delle offerte e delle partenze/disponibilità, e i relativi endpoint REST usati dall'applicazione.
    Frontend: Ho sviluppato le schermate dell'app Android in Jetpack Compose per la visualizzazione del catalogo e i dettagli dei viaggi, oltre ai flussi dedicati all'organizzatore per visualizzare, inserire e gestire le proprie offerte.
    ho gestito anche la parte dell’admin e la sua rispettiva schermata.
---

## Indice

1. [Come funziona l'applicazione](#1-come-funziona-lapplicazione)
2. [Architettura](#2-architettura)
3. [Prerequisiti](#3-prerequisiti)
4. [Avvio dei servizi (backend, Keycloak, Mailpit)](#4-avvio-dei-servizi-backend-keycloak-mailpit)
5. [Avvio dell'app Android](#5-avvio-dellapp-android)
6. [Percorso di prova consigliato](#6-percorso-di-prova-consigliato)
7. [Come creare un utente ADMIN](#7-come-creare-un-utente-admin)
8. [Esecuzione dei test](#8-esecuzione-dei-test)
9. [Struttura del repository](#9-struttura-del-repository)

---

## 1. Come funziona l'applicazione

### Ruoli

L'autorizzazione si basa **esclusivamente sui ruoli presenti nel token JWT** emesso da Keycloak
(claim `realm_access.roles`), non su un campo del database.

| Ruolo | Cosa può fare |
|---|---|
| **VIAGGIATORE** | Esplora il catalogo, salva preferiti e liste condivise, prenota e paga, riceve notifiche, scrive recensioni sui viaggi conclusi |
| **ORGANIZZATORE** | Tutto quanto sopra, più: crea e modifica itinerari e attività, gestisce le partenze e le disponibilità, vede i prenotati e il saldo incassato |
| **ADMIN** | Gestione utenti e gestione completa delle offerte pubblicate |

`VIAGGIATORE` e `ORGANIZZATORE` si scelgono in fase di registrazione. `ADMIN` **non** è
richiedibile dalla registrazione: si assegna solo a mano (vedi [§7](#7-come-creare-un-utente-admin)).

### Flussi principali

- **Registrazione e login** — la registrazione avviene da dentro l'app (`POST /api/auth/registrazione`):
  il backend crea l'utente su Keycloak tramite l'Admin REST API e gli assegna il ruolo realm scelto.
  Il login vero e proprio non passa dal backend: l'app apre la pagina di Keycloak con flusso
  **Authorization Code + PKCE** e riceve un access token. Alla prima apertura è richiesta la
  **verifica dell'indirizzo email** (in sviluppo le mail vengono catturate da Mailpit).
- **Catalogo** — la lettura di itinerari e attività è pubblica; tutto il resto richiede il token.
  Un itinerario ha giorni di programma, tappe, attività extra acquistabili e una o più
  *disponibilità* (le partenze, con data, posti e prezzo).
- **Prenotazione e pagamento** — flusso a due passi nell'app (dati della prenotazione → metodo di
  pagamento), con schermata di conferma finale. La decurtazione dei posti è protetta da controlli
  di concorrenza lato server (test dedicati in `src/test/.../booking/service/*ConcorrenzaTest.java`).
- **Esperienza post-viaggio** — un job schedulato giornaliero (default: 09:00, `Europe/Rome`) crea
  una notifica in-app che invita a recensire i viaggi conclusi il giorno precedente. Il job è
  idempotente: rieseguirlo non genera doppioni. Le recensioni possono avere immagini allegate.
- **Immagini** — non stanno nel database e non sono servite come risorse statiche: i byte finiscono
  su disco (default) o su object storage S3-compatibile, e vengono restituiti solo dall'endpoint
  autenticato `GET /api/immagini/{id}/contenuto`.

### API principali

Documentazione interattiva (Swagger UI): **<http://localhost:8081/swagger-ui/index.html>**

| Prefisso | Contenuto |
|---|---|
| `/api/auth/registrazione` | Registrazione self-service (unica rotta pubblica sotto `/api`, oltre alle GET del catalogo) |
| `/api/utenti` | Profilo, sincronizzazione al primo login, cambio password, foto profilo, gestione utenti (ADMIN) |
| `/api/itinerari` | Catalogo viaggi, disponibilità, attività extra, recensioni, immagini |
| `/api/attivita` | Singole attività e relative sessioni |
| `/api/prenotazioni` | Prenotazioni dell'utente, annullamento, partenze e saldo lato organizzatore |
| `/api/pagamenti` | Pagamento di una prenotazione, elenco dei propri pagamenti |
| `/api/preferiti` | Liste di preferiti, con condivisione fra utenti |
| `/api/recensioni` | Recensioni, media delle valutazioni, immagini allegate |
| `/api/notifiche` | Notifiche in-app, lette e non lette |
| `/api/immagini` | Upload e download del contenuto binario |

### Sicurezza

Il backend è un **resource server stateless**: nessuna sessione, nessun cookie, solo bearer token.
Sono attivi fra le altre cose: validazione di issuer **e** audience del token, deny-by-default sulle
rotte, rate limiting in memoria per utente e per IP, tetto alla paginazione, limiti su dimensione e
risoluzione delle immagini caricate, rifiuto dei campi non previsti nei payload (mass assignment),
header di sicurezza e CORS con allow-list esplicita, audit log e `traceId` di correlazione.
Dettagli in [`docs/SECURITY.md`](docs/SECURITY.md) e [`docs/security-inventory.md`](docs/security-inventory.md).

---

## 2. Architettura

```
┌───────────────────────────┐
│  App Android (Kotlin)     │
│  Compose + Retrofit       │
│  AppAuth (PKCE)           │
└─────┬───────────────┬─────┘
      │ login OIDC    │ REST + Bearer JWT
      ▼               ▼
┌───────────┐   ┌──────────────────┐   ┌──────────────┐
│ Keycloak  │   │ Backend Spring   │──▶│ PostgreSQL   │
│  :8090    │◀──│ Boot 4   :8081   │   │              │
└─────┬─────┘   └────────┬─────────┘   └──────────────┘
      │ SMTP             │ file
      ▼                  ▼
┌───────────┐   ┌──────────────────┐
│ Mailpit   │   │ storage-immagini │
│  :8025    │   │  (oppure S3)     │
└───────────┘   └──────────────────┘
```

| Componente | Tecnologia |
|---|---|
| Backend | Java 17, Spring Boot 4, Spring Security (OAuth2 Resource Server), Spring Data JPA, springdoc-openapi, Bucket4j |
| Database | PostgreSQL (schema generato da Hibernate, `ddl-auto=update`) |
| Identity provider | Keycloak 24, realm `travelapp` importato da `keycloak-import/travelapp-realm.json` |
| Posta di sviluppo | Mailpit (cattura le mail di verifica e di reset password) |
| App | Kotlin, Jetpack Compose + Material 3, Navigation Compose, ViewModel, Retrofit/OkHttp, AppAuth, DataStore, Coil, Lottie |
| Test | JUnit 5, Spring Security Test, Testcontainers (con fallback automatico su H2) |

Il backend è organizzato in quattro moduli funzionali — `identity`, `catalog`, `booking`,
`experience` — più `config`, `common` ed `exception` trasversali; ognuno con
controller / service / repository / entity / dto / mapper.

---

## 3. Prerequisiti

| Serve | Versione | Note |
|---|---|---|
| **Docker Desktop** | recente, con `docker compose` | avvia backend, Keycloak e Mailpit |
| **Android Studio** | Ladybug o successivo | per compilare ed eseguire l'app (compileSdk 36, minSdk 24) |
| **PostgreSQL** | 14+ | un'istanza raggiungibile: cloud oppure locale (vedi passo 2) |
| Git | — | per clonare il repository |

Non serve installare Java o Maven: il backend viene compilato dentro Docker
(`Dockerfile`, `eclipse-temurin:17`). Servono invece se si vuole lanciare il backend dall'IDE o
eseguire i test — e in quel caso basta il wrapper `./mvnw`, che scarica Maven da sé.

---

## 4. Avvio dei servizi (backend, Keycloak, Mailpit)

### Passo 1 — clonare il repository

```bash
git clone <url-del-repository>
cd travelapp
```

### Passo 2 — creare il file `.env`

variabili d'ambiente da inserire:
```declarative
DB_URL=jdbc:postgresql://ep-dry-sun-aleh5jne-pooler.c-3.eu-central-1.aws.neon.tech/neondb?sslmode\=require&channel_binding\=require&allowPublicKeyRetrival\=true
DB_USERNAME=neondb_owner
DB_PASSWORD=npg_ChMeBZA7tH5l

APP_STORAGE_IMMAGINI_TIPO=s3
APP_STORAGE_S3_ACCESS_KEY_ID=df90fe40cb6a138225572c391f36b466
APP_STORAGE_S3_BUCKET=travelapp-photo
APP_STORAGE_S3_ENDPOINT=https://aec065ac79464c5fc64737616603dae3.r2.cloudflarestorage.com
APP_STORAGE_S3_SECRET_ACCESS_KEY=623145e4298aae19c2bdedcf401cec7025d0e226f43b6e2b608315440763ac3d
```


### Passo 3 — primo avvio

```bash
docker compose up --build
```

La prima esecuzione compila il backend e scarica le immagini Docker: richiede qualche minuto.
Al termine sono attivi:

| Servizio | Indirizzo | Credenziali |
|---|---|---|
| Backend / Swagger UI | <http://localhost:8081/swagger-ui/index.html> | — |
| Console Keycloak | <http://localhost:8090> | `admin` / `admin` |
| Casella di posta (Mailpit) | <http://localhost:8025> | — |

Keycloak importa da solo il realm `travelapp` con i tre client applicativi
(`travelapp-android`, `travelapp-backend`, `travelapp-registration`) e i ruoli
`VIAGGIATORE`, `ORGANIZZATORE`, `ADMIN`.

### Passo 4 — client secret della registrazione (obbligatorio per potersi registrare)

I segreti non sono versionati: nel file di import compaiono mascherati, quindi Keycloak ne genera
uno nuovo su ogni realm creato da zero. Va letto e passato al backend, altrimenti
`POST /api/auth/registrazione` risponde `503` (il resto dell'applicazione funziona comunque).

1. Aprire <http://localhost:8090> e accedere con `admin` / `admin`
2. In alto a sinistra selezionare il realm **travelapp**
3. **Clients** → `travelapp-registration` → scheda **Credentials** → copiare il *Client secret*
4. Incollarlo nel `.env`:

   ```properties
   KEYCLOAK_ADMIN_CLIENT_SECRET=<secret copiato>
   ```

5. Ricaricare il backend con la nuova variabile:

   ```bash
   docker compose up -d --force-recreate backend
   ```

### Verifica rapida

```bash
curl http://localhost:8081/api/itinerari
```

Deve rispondere con una pagina JSON (all'inizio vuota): il catalogo in lettura è pubblico.
Una chiamata a una rotta protetta senza token deve invece restituire `401`.

---

## 5. Avvio dell'app Android

### Passo 1 — aprire il progetto

In Android Studio: **Open** → selezionare la cartella **`travelApp_frontEnd`** (non la radice del
repository). Attendere la sincronizzazione Gradle.

### Passo 2 — configurare gli indirizzi (solo se serve)

Backend e Keycloak sono letti da `travelApp_frontEnd/local.properties`, che non è versionato. I
valori predefiniti sono `http://localhost:8081/` e `http://localhost:8090`, e vanno già bene per:

- l'**emulatore** Android;
- un **telefono fisico collegato via USB** con il debug attivo.

In entrambi i casi il progetto esegue automaticamente `adb reverse` sulle porte 8081, 8090 e 8025
prima di `installDebug` / `assembleDebug`, quindi non c'è nulla da configurare.

Solo se si vuole usare un telefono **attraverso la rete Wi-Fi** invece che via cavo, copiare
`local.properties.example` in `local.properties` e impostare l'IP della macchina di sviluppo:

```properties
backend.base.url=http://192.168.1.10:8081/
keycloak.base.url=http://192.168.1.10:8090
```

Lo **stesso** indirizzo va messo anche in `KEYCLOAK_PUBLIC_URL` nel `.env`, riavviando poi i
servizi: quel valore fissa l'`issuer` dei token, e il backend rifiuta i token che dichiarano un
issuer diverso da quello configurato.

### Passo 3 — eseguire

Premere **Run** (`Shift+F10`) con un emulatore avviato o un dispositivo collegato.
L'app parte sulla schermata **Explore**; da lì si accede a login e registrazione.

---

## 6. Percorso di prova consigliato

Un giro completo che tocca tutte le parti del sistema:

1. **Registrare un organizzatore** — dall'app, scegliendo il ruolo *Organizzatore*.
2. **Verificare l'email** — aprire <http://localhost:8025>, aprire il messaggio di TravelApp e
   cliccare il link di verifica. È un passaggio obbligatorio: senza, il login resta bloccato.
3. **Accedere** e creare un itinerario o un'attività, con almeno una partenza (o sessione), un
   prezzo, dei posti disponibili e un'immagine di copertina.
4. **Registrare un secondo utente**, stavolta come *viaggiatore*, verificando di nuovo l'email su
   Mailpit, e accedere con quello.
5. **Esplorare il catalogo**, aprire il dettaglio dell'offerta creata al punto 3, salvarla nei
   preferiti, quindi **prenotare** e completare il **pagamento**.
6. Tornare sull'account organizzatore per vedere la prenotazione fra i prenotati della partenza e
   l'importo nel saldo.
7. **Recensioni**: sono possibili sui viaggi conclusi. Per provarle senza aspettare conviene
   creare una partenza con date già passate e prenotarla.

Per interrogare le API direttamente c'è la collection Postman in
[`postman/collections/`](postman/collections), oltre alla Swagger UI.

---

## 7. Come creare un utente ADMIN

Il ruolo `ADMIN` non è ottenibile dalla registrazione. Si assegna dalla console Keycloak:

1. <http://localhost:8090> → login `admin` / `admin` → realm **travelapp**
2. **Users** → selezionare l'utente → scheda **Role mapping** → **Assign role**
3. Filtrare per ruoli del realm, selezionare **ADMIN** e assegnarlo
4. Nell'app: **logout e nuovo login** — il ruolo viaggia nel token, e quello già emesso non lo contiene

Da quel momento l'utente vede la dashboard di amministrazione, con gestione utenti e gestione delle
offerte pubblicate. Un ADMIN può poi promuoverne altri con `PUT /api/utenti/{id}/ruolo/admin`.

---

## 8. Esecuzione dei test

Dalla radice del repository:

```bash
./mvnw test
```

```powershell
# Windows
.\mvnw.cmd test
```

I test non richiedono né il database di sviluppo né Keycloak: usano **Testcontainers** con
PostgreSQL se Docker risponde, altrimenti ricadono automaticamente su **H2** in modalità
PostgreSQL. I token JWT sono firmati da un server JWK di prova.

La suite copre i servizi di dominio — compresi gli scenari di concorrenza su posti e pagamenti — e
una batteria di test di sicurezza organizzata per fasi, in
`src/test/java/com/unical/travelapp/backend/security/`: autorizzazione e IDOR, validazione
dell'input, limiti sulle risorse, formato degli errori, audit, rete e CORS. Il resoconto è in
[`docs/security-test-report.md`](docs/security-test-report.md).

I test marcati `keycloak-live`, che richiedono un'istanza Keycloak reale già configurata, sono
esclusi dall'esecuzione predefinita.

---

## 9. Struttura del repository

```
travelapp/
├── docker-compose.yml           backend + Keycloak + Mailpit
├── Dockerfile                   build multi-stage del backend
├── .env.example                 modello della configurazione locale
├── pom.xml                      dipendenze del backend
├── keycloak-import/             realm travelapp (client, ruoli, policy)
├── docs/                        sicurezza, setup Keycloak, report dei test
├── postman/                     collection per provare le API
├── storage-immagini/            immagini caricate (default, non versionate)
├── src/
│   ├── main/java/.../backend/
│   │   ├── identity/            utenti, registrazione, integrazione Keycloak
│   │   ├── catalog/             itinerari, attività, disponibilità, sessioni
│   │   ├── booking/             prenotazioni e pagamenti
│   │   ├── experience/          recensioni, preferiti, immagini, notifiche
│   │   ├── config/              sicurezza, CORS, rate limit, OpenAPI, scheduling
│   │   ├── common/audit/        audit log
│   │   └── exception/           gestione centralizzata degli errori
│   └── test/                    test di dominio e di sicurezza
└── travelApp_frontEnd/          progetto Android (aprire QUESTA cartella in Android Studio)
    └── app/src/main/java/com/example/travelapp/
        ├── data/remote/         Retrofit, DTO, sessione e token
        ├── data/repository/     repository
        ├── domain/model/        modelli di dominio
        └── ui/                  schermate Compose, ViewModel, navigazione, tema
```

---


