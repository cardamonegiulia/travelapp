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

data class SessioneKeycloak(
    val accessToken: String,
    val refreshToken: String?,
    val scadenzaMs: Long?
)

sealed class EsitoRinnovo {

    data class Successo(
        val sessione: SessioneKeycloak
    ) : EsitoRinnovo()

    object Rifiutato : EsitoRinnovo()

    object ErroreRete : EsitoRinnovo()
}

object KeycloakManager {

    private const val CLIENT_ID = "travelapp-android"

    private val KEYCLOAK_BASE =
        BuildConfig.KEYCLOAK_BASE_URL
            .trimEnd('/') +
                "/realms/travelapp"

    private val TOKEN_ENDPOINT =
        "$KEYCLOAK_BASE/protocol/openid-connect/token"

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

    private val appAuthConfig =
        AppAuthConfiguration.Builder()
            .setConnectionBuilder(
                LocalConnectionBuilder
            )
            .build()

    private val httpRinnovo by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private var authService: AuthorizationService? = null

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
        }
    }

    fun estraiRuolo(
        accessToken: String
    ): String = LettoreToken.ruolo(accessToken)
}
