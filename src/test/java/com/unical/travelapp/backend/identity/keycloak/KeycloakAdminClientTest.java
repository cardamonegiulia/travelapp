package com.unical.travelapp.backend.identity.keycloak;

import com.unical.travelapp.backend.identity.exception.IdentityProviderNonDisponibileException;
import com.unical.travelapp.backend.identity.exception.PasswordNonConformeException;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.NuovoUtenteKeycloak;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.ProfiloKeycloak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Cosa viene davvero mandato all'Admin API di Keycloak.
 *
 * <p>Negli altri test {@link KeycloakAdminClient} e' sostituito da un mock, il che verifica
 * <i>che</i> venga chiamato ma non <i>con che corpo</i>. Qui si sostituisce invece il
 * trasporto HTTP: e' l'unico punto in cui si controlla la richiesta effettiva, e serve
 * perche' campi come {@code emailVerified} decidono se una persona puo' rivendicare
 * l'indirizzo di un'altra.
 */
class KeycloakAdminClientTest {

    private static final String BASE_URL = "https://idp.test.local";
    private static final String REALM = "travelapp";
    private static final String CLIENT_ID = "travelapp-registration";
    private static final String SEGRETO_DI_PROVA = "valore-fittizio-per-il-test";
    private static final String TOKEN_ADMIN = "token-di-servizio";
    private static final String KEYCLOAK_ID = "b1f0c5a2-0000-4000-8000-000000000001";

    private static final String UTENTI = BASE_URL + "/admin/realms/" + REALM + "/users";

    private MockRestServiceServer keycloak;
    private KeycloakAdminClient client;

    @BeforeEach
    void preparaIlTrasporto() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        keycloak = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakAdminClient(builder.build(), REALM, CLIENT_ID, SEGRETO_DI_PROVA);
    }

    private NuovoUtenteKeycloak nuovoUtente() {
        return new NuovoUtenteKeycloak("mario@example.test", "mario@example.test",
                "Mario", "Rossi", "PasswordSicura1");
    }

    // --- creazione ---------------------------------------------------------

    @Test
    void lUtenteCreatoHaLEmailNonVerificata() {
        keycloak.expect(requestTo(UTENTI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.enabled").value(true))
                .andRespond(withCreatedEntity(URI.create(UTENTI + "/" + KEYCLOAK_ID)));

        assertThat(client.creaUtente(TOKEN_ADMIN, nuovoUtente())).isEqualTo(KEYCLOAK_ID);
        keycloak.verify();
    }

    @Test
    void laPasswordVieneImpostataComeDefinitiva() {
        keycloak.expect(requestTo(UTENTI))
                .andExpect(jsonPath("$.credentials[0].type").value("password"))
                .andExpect(jsonPath("$.credentials[0].temporary").value(false))
                .andRespond(withCreatedEntity(URI.create(UTENTI + "/" + KEYCLOAK_ID)));

        client.creaUtente(TOKEN_ADMIN, nuovoUtente());
        keycloak.verify();
    }

    @Test
    void unEmailGiaPresenteSulRealmDiventaUnConflitto() {
        keycloak.expect(requestTo(UTENTI)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.creaUtente(TOKEN_ADMIN, nuovoUtente()))
                .isInstanceOf(UtenteGiaEsistenteException.class)
                .hasMessageNotContaining("Keycloak");
    }

    // --- aggiornamento del profilo -----------------------------------------

    @Test
    void lAggiornamentoDelProfiloNonRiscriveLoUsername() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.email").value("nuova@example.test"))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                // lo username e' l'identificativo con cui una persona fa login: cambiarlo
                // sotto i piedi dell'utente e' una decisione diversa dall'aggiornare l'email
                .andExpect(jsonPath("$.username").doesNotExist())
                // ne' si toccano stato dell'account e credenziali
                .andExpect(jsonPath("$.enabled").doesNotExist())
                .andExpect(jsonPath("$.credentials").doesNotExist())
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.aggiornaProfilo(TOKEN_ADMIN, KEYCLOAK_ID,
                new ProfiloKeycloak("nuova@example.test", "Ada", "Lovelace"));
        keycloak.verify();
    }

    @Test
    void unEmailDiUnAltroUtenteFaFallireLAggiornamentoConUnConflitto() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.aggiornaProfilo(TOKEN_ADMIN, KEYCLOAK_ID,
                new ProfiloKeycloak("presa@example.test", "Ada", "Lovelace")))
                .isInstanceOf(UtenteGiaEsistenteException.class);
    }

    // --- cancellazione -----------------------------------------------------

    @Test
    void laCancellazioneDiUnUtenteGiaAssenteNonEUnErrore() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatCode(() -> client.eliminaUtente(TOKEN_ADMIN, KEYCLOAK_ID))
                .as("operazione idempotente: il record locale resta comunque ripulibile")
                .doesNotThrowAnyException();
    }

    @Test
    void unaCancellazioneRifiutataVieneSegnalata() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.eliminaUtente(TOKEN_ADMIN, KEYCLOAK_ID))
                .isInstanceOf(IdentityProviderNonDisponibileException.class);
    }

    @Test
    void laCompensazioneDelProfiloNonPropagaErrori() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatCode(() -> client.aggiornaProfiloSenzaPropagareErrori(TOKEN_ADMIN, KEYCLOAK_ID,
                new ProfiloKeycloak("vecchia@example.test", "Mario", "Rossi")))
                .as("l'errore da riportare al chiamante e' quello originale, non quello della pulizia")
                .doesNotThrowAnyException();
    }

    // --- cambio password ---------------------------------------------------

    @Test
    void laNuovaPasswordVieneImpostataComeDefinitiva() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID + "/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.type").value("password"))
                // temporanea significherebbe chiedere all'utente, al login successivo, di
                // cambiare la password che ha appena scelto
                .andExpect(jsonPath("$.temporary").value(false))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.impostaPassword(TOKEN_ADMIN, KEYCLOAK_ID, "NuovaPassword1");
        keycloak.verify();
    }

    @Test
    void unaPasswordRifiutataDallaPolicyEUnErroreDelChiamante() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID + "/reset-password"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.impostaPassword(TOKEN_ADMIN, KEYCLOAK_ID, "NuovaPassword1"))
                .as("400 dal realm significa password debole, non servizio guasto")
                .isInstanceOf(PasswordNonConformeException.class);
    }

    @Test
    void laChiusuraDelleSessioniNonInterrompeIlCambioPassword() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID + "/logout"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatCode(() -> client.terminaSessioniSenzaPropagareErrori(TOKEN_ADMIN, KEYCLOAK_ID))
                .as("la password nuova e' gia' attiva: un errore qui non va rilanciato al chiamante")
                .doesNotThrowAnyException();
    }

    // --- token del service account -----------------------------------------

    @Test
    void senzaClientSecretNonSiContattaNemmenoKeycloak() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer nessunaChiamata = MockRestServiceServer.bindTo(builder).build();
        KeycloakAdminClient senzaSegreto = new KeycloakAdminClient(builder.build(), REALM, CLIENT_ID, "");

        assertThatThrownBy(senzaSegreto::ottieniTokenAmministrativo)
                .isInstanceOf(IdentityProviderNonDisponibileException.class);

        nessunaChiamata.verify();
    }

    @Test
    void ilTokenDelServiceAccountViaggiaSulTokenEndpointDelRealm() {
        keycloak.expect(requestTo(BASE_URL + "/realms/" + REALM + "/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"" + TOKEN_ADMIN + "\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        assertThat(client.ottieniTokenAmministrativo()).isEqualTo(TOKEN_ADMIN);
        keycloak.verify();
    }
}
