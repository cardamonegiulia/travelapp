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

    @Test
    void unaPasswordRifiutataDallaPolicyNonEUnGuastoDelServizio() {
        keycloak.expect(requestTo(UTENTI)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.creaUtente(TOKEN_ADMIN, nuovoUtente()))
                .as("diventava un 503: chi si registra riprovava con la stessa password "
                        + "all'infinito senza sapere che il problema era quella")
                .isInstanceOf(PasswordNonConformeException.class)
                .hasMessageNotContaining("Keycloak");
    }

    @Test
    void lAggiornamentoDelProfiloNonRiscriveLoUsername() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.email").value("nuova@example.test"))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.enabled").doesNotExist())
                .andExpect(jsonPath("$.credentials").doesNotExist())
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.aggiornaProfilo(TOKEN_ADMIN, KEYCLOAK_ID,
                new ProfiloKeycloak("nuova@example.test", "Ada", "Lovelace"), true);
        keycloak.verify();
    }

    @Test
    void unEmailDiUnAltroUtenteFaFallireLAggiornamentoConUnConflitto() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.aggiornaProfilo(TOKEN_ADMIN, KEYCLOAK_ID,
                new ProfiloKeycloak("presa@example.test", "Ada", "Lovelace"), true))
                .isInstanceOf(UtenteGiaEsistenteException.class);
    }

    @Test
    void unIndirizzoNuovoNonEreditaLaVerificaDelVecchio() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.aggiornaProfilo(TOKEN_ADMIN, KEYCLOAK_ID,
                new ProfiloKeycloak("nuova@example.test", "Ada", "Lovelace"), true);
        keycloak.verify();
    }

    @Test
    void cambiareSoloNomeECognomeNonFaRiverificareLEmail() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.emailVerified").doesNotExist())
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.aggiornaProfilo(TOKEN_ADMIN, KEYCLOAK_ID,
                new ProfiloKeycloak("stessa@example.test", "Ada", "Lovelace"), false);
        keycloak.verify();
    }

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

    @Test
    void laNuovaPasswordVieneImpostataComeDefinitiva() {
        keycloak.expect(requestTo(UTENTI + "/" + KEYCLOAK_ID + "/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.type").value("password"))
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
