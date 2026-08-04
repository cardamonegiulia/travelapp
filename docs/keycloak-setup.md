# Configurazione Keycloak richiesta (Fase 1)

Queste modifiche vanno applicate manualmente sull'istanza Keycloak (realm `travelapp`,
console admin) o aggiunte al file di import `keycloak-import/travelapp-realm.json` a cura
del team che gestisce l'ambiente. Non vengono applicate automaticamente dal codice.

## 1. Audience nel token (obbligatorio, breaking change)

Dalla Fase 1c il backend valida il claim `aud` del JWT (proprietà
`app.security.expected-audience`, default `travelapp-backend`). **Senza questa modifica
lato Keycloak, tutte le richieste falliranno con 401 `invalid_token`**, perché di default
i client Keycloak non includono automaticamente il proprio `client_id` in `aud`.

Passi (console admin Keycloak → realm `travelapp`):

1. Clients → `travelapp-backend` → Client scopes → `travelapp-backend-dedicated` → Add mapper → By configuration
2. Scegli **Audience**
3. Configura:
   - Name: `aud-travelapp-backend`
   - Included Client Audience: `travelapp-backend`
   - Add to ID token: off
   - Add to access token: **on**
4. Salva

Verifica: decodifica un access token ottenuto per questo client su jwt.io e controlla che
`aud` contenga `"travelapp-backend"`.

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

## Riepilogo proprietà applicative coinvolte

| Property | Default | Env var override |
|---|---|---|
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://localhost:8090/realms/travelapp` | `OAUTH2_ISSUER_URI` |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `.../protocol/openid-connect/certs` | `OAUTH2_JWK_SET_URI` |
| `app.security.expected-audience` | `travelapp-backend` | `SECURITY_EXPECTED_AUDIENCE` |
| `app.security.resource-client-id` | `travelapp-backend` | `SECURITY_RESOURCE_CLIENT_ID` |

In produzione, `issuer-uri` deve essere sempre HTTPS (oggi `sslRequired: "external"` nel
realm consente HTTP per richieste "interne": da rivedere in Fase 6 per un deploy reale).
