package com.example.travelapp.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.travelapp.BuildConfig
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Sessione ottenuta da Keycloak: oltre all'access token conserviamo il
 * refresh token e l'istante di scadenza, indispensabili per restare
 * autenticati dopo la chiusura dell'app.
 */
data class SessioneKeycloak(
    val accessToken: String,
    val refreshToken: String?,
    val scadenzaMs: Long?
)

/**
 * Esito del rinnovo tramite refresh token.
 *
 * Distinguiamo un rifiuto di Keycloak (refresh token scaduto o revocato:
 * l'utente deve rifare il login) da un problema di rete temporaneo, che non
 * deve buttare fuori l'utente.
 */
sealed class EsitoRinnovo {

    data class Successo(
        val sessione: SessioneKeycloak
    ) : EsitoRinnovo()

    object Rifiutato : EsitoRinnovo()

    object ErroreRete : EsitoRinnovo()
}

object KeycloakManager {

    private const val CLIENT_ID = "travelapp-android"

    /**
     * Indirizzo di Keycloak iniettato a build time da local.properties.
     *
     * In questo modo ogni sviluppatore può usare localhost,
     * adb reverse, emulatore o IP LAN senza modificare il sorgente.
     */
    private val KEYCLOAK_BASE =
        BuildConfig.KEYCLOAK_BASE_URL
            .trimEnd('/') +
                "/realms/travelapp"

    private val TOKEN_ENDPOINT =
        "$KEYCLOAK_BASE/protocol/openid-connect/token"

    /**
     * Deve coincidere con una Valid Redirect URI
     * configurata nel client travelapp-android.
     */
    private const val REDIRECT_URI =
        "com.example.travelapp:/oauth2redirect"

    private val serviceConfig =
        AuthorizationServiceConfiguration(
            Uri.parse(
                "$KEYCLOAK_BASE/protocol/openid-connect/auth"
            ),
            Uri.parse(
                TOKEN_ENDPOINT
            )
        )

    /**
     * Permette l'uso di Keycloak via HTTP durante lo sviluppo locale.
     */
    private val appAuthConfig =
        AppAuthConfiguration.Builder()
            .setConnectionBuilder(
                LocalConnectionBuilder
            )
            .build()

    /**
     * Client dedicato al solo rinnovo del token: non passa
     * dall'InterceptorAutenticazione, altrimenti si rientrerebbe
     * nel rinnovo mentre lo si sta già eseguendo.
     */
    private val httpRinnovo by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Manteniamo una sola istanza attiva alla volta.
     */
    private var authService: AuthorizationService? = null

    /**
     * Crea l'intent per il login Keycloak.
     *
     * loginHint:
     * precompila username/email quando disponibile.
     *
     * forzaLogin:
     * forza Keycloak a mostrare nuovamente la schermata di login
     * invece di riutilizzare automaticamente una sessione precedente.
     */
    fun creaIntentLogin(
        context: Context,
        loginHint: String? = null,
        forzaLogin: Boolean = false
    ): Intent {

        authService?.dispose()

        val servizio =
            AuthorizationService(
                context,
                appAuthConfig
            )

        authService = servizio

        val requestBuilder =
            AuthorizationRequest.Builder(
                serviceConfig,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(REDIRECT_URI)
            )
                .setScopes(
                    "openid",
                    "profile",
                    "email",
                    // Chiede un refresh token offline: sopravvive alla
                    // scadenza della sessione SSO (30 minuti di inattività)
                    // e permette di restare loggati tra un avvio e l'altro.
                    "offline_access"
                )

        if (!loginHint.isNullOrBlank()) {
            requestBuilder.setLoginHint(
                loginHint
            )
        }

        if (forzaLogin) {
            requestBuilder.setPrompt("login")
        }

        return servizio
            .getAuthorizationRequestIntent(
                requestBuilder.build()
            )
    }

    fun scambiaCodicePToken(
        context: Context,
        intent: Intent,
        onSuccess: (sessione: SessioneKeycloak) -> Unit,
        onError: (String) -> Unit
    ) {

        val servizio =
            authService
                ?: AuthorizationService(
                    context,
                    appAuthConfig
                )

        val response =
            AuthorizationResponse
                .fromIntent(intent)

        val error =
            AuthorizationException
                .fromIntent(intent)

        if (error != null) {

            servizio.dispose()
            authService = null

            onError(
                "Errore login: ${error.message}"
            )

            return
        }

        if (response == null) {

            servizio.dispose()
            authService = null

            onError(
                "Risposta Keycloak vuota"
            )

            return
        }

        servizio.performTokenRequest(
            response.createTokenExchangeRequest()
        ) { tokenResponse, ex ->

            servizio.dispose()
            authService = null

            if (ex != null) {
                onError(
                    "Errore token: ${ex.message}"
                )
                return@performTokenRequest
            }

            val token =
                tokenResponse?.accessToken

            if (token != null) {
                onSuccess(
                    SessioneKeycloak(
                        accessToken = token,
                        refreshToken = tokenResponse.refreshToken,
                        scadenzaMs = tokenResponse.accessTokenExpirationTime
                    )
                )
            } else {
                onError(
                    "Token non ricevuto"
                )
            }
        }
    }

    /**
     * Rinnova l'access token a partire dal refresh token.
     *
     * È una chiamata HTTP sincrona verso il token endpoint: viene invocata
     * da thread di background (interceptor OkHttp o coroutine su IO), quindi
     * non passiamo da AppAuth, che consegnerebbe il risultato sul main thread
     * e bloccherebbe l'interceptor in attesa di se stesso.
     *
     * Se Keycloak rifiuta il refresh token l'utente deve rifare il login;
     * se invece la rete non è raggiungibile la sessione resta valida e si
     * riproverà alla richiesta successiva.
     */
    fun rinnovaAccessToken(
        refreshToken: String
    ): EsitoRinnovo {

        return try {

            val corpo = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("refresh_token", refreshToken)
                .build()

            val richiesta = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(corpo)
                .build()

            httpRinnovo.newCall(richiesta).execute().use { risposta ->

                if (!risposta.isSuccessful) {
                    // 400/401: refresh token scaduto o revocato.
                    // Qualsiasi altro codice è un problema del server,
                    // quindi vale come errore temporaneo.
                    return if (risposta.code in 400..401) {
                        EsitoRinnovo.Rifiutato
                    } else {
                        EsitoRinnovo.ErroreRete
                    }
                }

                val json = JSONObject(
                    risposta.body?.string().orEmpty()
                )

                val nuovoAccessToken =
                    json.optString("access_token")
                        .takeIf { it.isNotBlank() }
                        ?: return EsitoRinnovo.Rifiutato

                val secondiValidita =
                    json.optLong("expires_in", 0L)

                EsitoRinnovo.Successo(
                    SessioneKeycloak(
                        accessToken = nuovoAccessToken,
                        refreshToken = json.optString("refresh_token")
                            .takeIf { it.isNotBlank() }
                            ?: refreshToken,
                        scadenzaMs = if (secondiValidita > 0)
                            System.currentTimeMillis() + secondiValidita * 1000
                        else
                            null
                    )
                )
            }

        } catch (e: Exception) {
            EsitoRinnovo.ErroreRete
        }
    }

    /**
     * Invalida la sessione lato Keycloak.
     *
     * Al logout non basta cancellare i token dal dispositivo: il refresh
     * token offline resterebbe valido per giorni. Chiamata sincrona, da
     * eseguire su un thread di background.
     */
    fun revocaSessione(refreshToken: String) {

        try {

            val corpo = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("refresh_token", refreshToken)
                .build()

            val richiesta = Request.Builder()
                .url("$KEYCLOAK_BASE/protocol/openid-connect/logout")
                .post(corpo)
                .build()

            httpRinnovo.newCall(richiesta).execute().close()

        } catch (e: Exception) {
            // Il logout locale avviene comunque: se il server non è
            // raggiungibile non ha senso bloccare l'utente nell'app.
        }
    }

    /**
     * Ricava il ruolo applicativo dal JWT.
     *
     * La lettura vera è in [LettoreToken], che guarda realm_access.roles
     * invece di cercare il nome del ruolo nel testo del token.
     */
    fun estraiRuolo(
        accessToken: String
    ): String = LettoreToken.ruolo(accessToken)
}
