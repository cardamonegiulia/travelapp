package com.unical.travelapp.backend.security.trasversali;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Il file di import del realm e' configurazione di sicurezza, e vale quanto la sua
 * riproducibilita': e' quello che ricrea Keycloak su una macchina nuova, in CI o dopo un
 * reset del volume. Le modifiche fatte a mano dalla console non lo aggiornano, quindi senza
 * un controllo automatico torna a divergere in silenzio.
 *
 * <p>Questi test non contattano Keycloak: leggono il file versionato e verificano che le
 * decisioni prese restino scritte.
 */
class ConfigurazioneRealmTest {

    private static final Path FILE_REALM = Path.of("keycloak-import/travelapp-realm.json");

    private static JsonNode realm;

    @BeforeAll
    static void leggiIlRealm() throws IOException {
        realm = new ObjectMapper().readTree(FILE_REALM.toFile());
    }

    @Test
    void ilRealmImponeUnaPolicySullePassword() {
        String policy = realm.path("passwordPolicy").asText();

        assertThat(policy)
                .as("senza policy sul realm le regole del backend valgono solo in registrazione: "
                        + "una password cambiata dall'account console le aggirerebbe")
                .isNotBlank();

        // deve restare allineata a RegistrazioneRequest: se il DTO accetta una password che
        // Keycloak rifiuta, l'utente riceve un errore alla creazione dell'account invece che
        // sulla validazione del form
        assertThat(policy)
                .contains("length(12)")
                .contains("digits(1)");
    }

    @Test
    void ilRealmProteggeIlLoginDagliAttacchiABruteForce() {
        assertThat(realm.path("bruteForceProtected").asBoolean())
                .as("il rate limit del backend non copre il login: quello avviene su Keycloak "
                        + "e non passa mai dall'applicazione")
                .isTrue();

        assertThat(realm.path("failureFactor").asInt())
                .as("il default di Keycloak (30 tentativi) e' troppo permissivo per una password")
                .isLessThanOrEqualTo(10);

        assertThat(realm.path("permanentLockout").asBoolean())
                .as("il blocco permanente trasforma un attacco in un disservizio per la vittima: "
                        + "meglio l'attesa incrementale")
                .isFalse();
    }

    @Test
    void ilRealmRichiedeLaVerificaDellEmail() {
        assertThat(realm.path("verifyEmail").asBoolean())
                .as("senza verifica chiunque puo' registrarsi con l'indirizzo di un altro")
                .isTrue();
    }

    @Test
    void ilRealmConsenteDiRecuperareUnaPasswordDimenticata() {
        assertThat(realm.path("resetPasswordAllowed").asBoolean())
                .as("senza questo flag un utente che dimentica la password non ha alcuna strada "
                        + "che non passi da un amministratore")
                .isTrue();
    }

    @Test
    void ilRealmHaUnServerSmtpConfigurato() {
        JsonNode smtp = realm.path("smtpServer");

        assertThat(smtp.path("host").asText())
                .as("verifica email e reset password mandano una mail: senza SMTP il realm "
                        + "impedirebbe il login senza dare modo di completarlo")
                .isNotBlank();
        assertThat(smtp.path("from").asText()).isNotBlank();
    }

    @Test
    void laRegistrazioneNativaDiKeycloakRestaSpenta() {
        assertThat(realm.path("registrationAllowed").asBoolean())
                .as("l'unica via di registrazione e' POST /api/auth/registrazione, che sceglie "
                        + "il ruolo da una lista bianca e crea anche il record locale")
                .isFalse();
    }

    @Test
    void iRuoliApplicativiEsistonoNelRealm() {
        assertThat(realm.path("roles").path("realm").findValuesAsText("name"))
                .as("senza questi ruoli la registrazione risponde 503 e i @PreAuthorize negano tutto")
                .contains("VIAGGIATORE", "ORGANIZZATORE", "ADMIN");
    }

    // --- separazione dei client: uno per mestiere ---------------------------

    @Test
    void ilResourceServerNonEmetteToken() {
        JsonNode backend = client("travelapp-backend");

        assertThat(backend.path("standardFlowEnabled").asBoolean())
                .as("travelapp-backend riceve i token e li valida: se emettesse anche quelli "
                        + "degli utenti, un secret compromesso darebbe insieme validazione e rilascio")
                .isFalse();
        assertThat(backend.path("directAccessGrantsEnabled").asBoolean())
                .as("il password grant fa passare la password dell'utente dentro l'applicazione, "
                        + "e taglia fuori MFA e verifica email")
                .isFalse();
    }

    @Test
    void ilClientDellAppAndroidEPubblicoEUsaPkce() {
        JsonNode android = client("travelapp-android");

        assertThat(android.path("publicClient").asBoolean())
                .as("un APK e' distribuito agli utenti: qualunque secret al suo interno si "
                        + "estrae in un minuto, quindi il client non deve averne")
                .isTrue();
        assertThat(android.path("attributes").path("pkce.code.challenge.method").asText())
                .as("senza secret, PKCE e' l'unica cosa che protegge lo scambio code -> token; "
                        + "lasciarlo vuoto significa 'facoltativo', e un attaccante sceglie la strada senza")
                .isEqualTo("S256");
        assertThat(android.path("implicitFlowEnabled").asBoolean())
                .as("l'implicit flow consegna il token nell'URL di redirect, dove finisce nei log "
                        + "e nella cronologia")
                .isFalse();
    }

    @Test
    void leRedirectUriDelClientPubblicoNonUsanoWildcard() {
        JsonNode redirectUris = client("travelapp-android").path("redirectUris");

        assertThat(redirectUris).as("senza redirect URI il flusso di login non parte").isNotEmpty();

        redirectUris.forEach(uri ->
                assertThat(uri.asText())
                        .as("su Android piu' app possono dichiarare lo stesso schema custom: una "
                                + "redirect URI permissiva aiuta un'app malevola a farsi consegnare "
                                + "l'authorization code")
                        .doesNotContain("*"));
    }

    @Test
    void leRedirectUriUsanoLoSchemaCustomDichiaratoDallApp() throws IOException {
        String schema = schemaRedirectDelloApp();

        assertThat(schema)
                .as("senza 'appAuthRedirectScheme' nel build.gradle.kts l'app non registra "
                        + "nessuno schema e Android non sa a chi consegnare il redirect")
                .isNotBlank();

        client("travelapp-android").path("redirectUris").forEach(uri ->
                assertThat(uri.asText())
                        .as("Keycloak confronta la redirect URI per intero: se il realm registra uno "
                                + "schema e l'app ne chiede un altro, il login muore su 'Invalid parameter: "
                                + "redirect_uri' prima ancora della form di login. Da Postman non si vede, "
                                + "perche' il password grant non usa nessuna redirect URI")
                        .startsWith(schema + ":"));
    }

    /** Lo schema custom registrato dall'app, letto dal {@code build.gradle.kts} del modulo Android. */
    private static String schemaRedirectDelloApp() throws IOException {
        String gradle = Files.readString(Path.of("travelApp_frontEnd/app/build.gradle.kts"));
        Matcher m = Pattern.compile("appAuthRedirectScheme\"\\]\\s*=\\s*\"([^\"]+)\"").matcher(gradle);
        return m.find() ? m.group(1) : "";
    }

    @Test
    void iTokenEmessiPerLAppPortanoLAudienceAttesaDalBackend() {
        JsonNode mapper = mapperAudience(client("travelapp-android"));

        assertThat(mapper)
                .as("il mapper audience vive sul client che EMETTE il token. Senza, 'aud' vale "
                        + "travelapp-android mentre AudienceValidator pretende travelapp-backend: "
                        + "ogni chiamata risponde 401, login riuscito compreso")
                .isNotNull();
        assertThat(mapper.path("config").path("included.client.audience").asText())
                .isEqualTo("travelapp-backend");
        assertThat(mapper.path("config").path("access.token.claim").asText())
                .as("e' l'access token quello che arriva al resource server, non l'id token")
                .isEqualTo("true");
    }

    @Test
    void iRuoliApplicativiSonoVeicolabiliDalClientDellApp() {
        JsonNode android = client("travelapp-android");

        assertThat(android.path("fullScopeAllowed").asBoolean())
                .as("'full scope allowed' mette nel token anche i ruoli dei client di sistema "
                        + "(account, realm-management): informazioni che l'app non usa, e il tipo "
                        + "di default permissivo che un giorno veicola un ruolo inatteso")
                .isFalse();

        // con lo scope ristretto e' il client a decidere quali ruoli puo' veicolare: se un
        // ruolo non e' elencato qui non arriva mai nel token, e ogni @PreAuthorize risponde 403
        assertThat(ruoliNelloScopeDi("travelapp-android"))
                .as("un realm ricreato da zero darebbe altrimenti token senza ruoli")
                .contains("VIAGGIATORE", "ORGANIZZATORE", "ADMIN");
    }

    /**
     * Il password grant e' la cosa che il disegno dei client vuole eliminare: fa passare la
     * password dell'utente dentro l'applicazione e taglia fuori MFA e verifica email.
     *
     * <p>L'invariante vale sui client applicativi. Fra i built-in di Keycloak
     * {@code admin-cli} resta pubblico e con i direct access grants accesi: e' il default di
     * ogni realm ed e' verificato a parte dal test successivo, che serve a non lasciarlo
     * passare in silenzio.
     */
    @Test
    void nessunClientApplicativoAccettaIlPasswordGrant() {
        realm.path("clients").forEach(client -> {
            String clientId = client.path("clientId").asText();
            if (clientId.startsWith("travelapp-") && client.path("publicClient").asBoolean()) {
                assertThat(client.path("directAccessGrantsEnabled").asBoolean())
                        .as("client pubblico '%s' con direct access grants: chiunque conosca il "
                                + "client id potrebbe provare coppie utente/password contro il realm",
                                clientId)
                        .isFalse();
            }
        });
    }

    /**
     * Promemoria eseguibile, non un'approvazione: finche' {@code admin-cli} resta com'e', il
     * password grant e' comunque disponibile su questo realm, e la separazione dei flussi
     * ottenuta sui client applicativi e' meno netta di quanto sembri. Il giorno in cui lo si
     * chiude, questo test diventa rosso e va cancellato: e' il momento in cui l'invariante
     * puo' tornare a valere su tutti i client.
     */
    @Test
    void ilPasswordGrantRestaApertoSuAdminCliPerDefaultDiKeycloak() {
        assertThat(client("admin-cli").path("directAccessGrantsEnabled").asBoolean())
                .as("se qui diventa false, generalizza nessunClientApplicativoAccettaIlPasswordGrant "
                        + "a tutti i client e cancella questo test")
                .isTrue();
    }

    /**
     * Il file di import descrive la <b>configurazione</b> del realm, non il suo materiale
     * crittografico: le chiavi sono diverse in ogni ambiente e Keycloak le genera da solo
     * quando non le trova.
     *
     * <p>Per un periodo ci sono finite dentro davvero, in chiaro. Chi legge il repository
     * puo' firmarsi da se' un access token con il {@code sub} di chiunque e il ruolo ADMIN:
     * la firma verifica contro la chiave pubblica pubblicata sul JWKS, quindi il backend lo
     * accetta come un token qualsiasi e nessun controllo applicativo se ne accorge. E'
     * l'aggiramento completo dell'autorizzazione, e non lascia traccia.
     */
    @Test
    void ilFileDiImportNonContieneChiaviDelRealm() {
        assertThat(realm.path("components").fieldNames())
                .toIterable()
                .as("le chiavi del realm non vanno versionate: Keycloak le rigenera all'import")
                .doesNotContain("org.keycloak.keys.KeyProvider");
    }

    @Test
    void ilClientDiTestNonEntraNelRealmRicreatoDaZero() {
        assertThat(realm.path("clients").findValuesAsText("clientId"))
                .as("travelapp-test esiste solo negli ambienti di sviluppo, creato a mano: se "
                        + "entrasse nel file di import seguirebbe il realm in produzione, "
                        + "lasciandoci aperto il password grant")
                .doesNotContain("travelapp-test");
    }

    // --- helper -------------------------------------------------------------

    private static JsonNode client(String clientId) {
        for (JsonNode client : realm.path("clients")) {
            if (clientId.equals(client.path("clientId").asText())) {
                return client;
            }
        }
        throw new AssertionError("client '" + clientId + "' assente dal file di import del realm");
    }

    private static JsonNode mapperAudience(JsonNode client) {
        for (JsonNode mapper : client.path("protocolMappers")) {
            if ("oidc-audience-mapper".equals(mapper.path("protocolMapper").asText())) {
                return mapper;
            }
        }
        return null;
    }

    private static List<String> ruoliNelloScopeDi(String clientId) {
        List<String> ruoli = new ArrayList<>();
        for (JsonNode mapping : realm.path("scopeMappings")) {
            if (clientId.equals(mapping.path("client").asText())) {
                mapping.path("roles").forEach(ruolo -> ruoli.add(ruolo.asText()));
            }
        }
        return ruoli;
    }
}
