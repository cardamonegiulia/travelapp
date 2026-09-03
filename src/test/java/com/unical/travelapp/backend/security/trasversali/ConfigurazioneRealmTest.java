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

        assertThat(ruoliNelloScopeDi("travelapp-android"))
                .as("un realm ricreato da zero darebbe altrimenti token senza ruoli")
                .contains("VIAGGIATORE", "ORGANIZZATORE", "ADMIN");
    }

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

    @Test
    void ilPasswordGrantRestaApertoSuAdminCliPerDefaultDiKeycloak() {
        assertThat(client("admin-cli").path("directAccessGrantsEnabled").asBoolean())
                .as("se qui diventa false, generalizza nessunClientApplicativoAccettaIlPasswordGrant "
                        + "a tutti i client e cancella questo test")
                .isTrue();
    }

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
