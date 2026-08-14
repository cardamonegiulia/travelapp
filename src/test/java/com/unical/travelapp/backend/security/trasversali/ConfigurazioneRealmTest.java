package com.unical.travelapp.backend.security.trasversali;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

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
}
