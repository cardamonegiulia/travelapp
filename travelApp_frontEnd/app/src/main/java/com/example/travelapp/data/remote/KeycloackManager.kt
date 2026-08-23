package com.example.travelapp.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.travelapp.BuildConfig
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

object KeycloakManager {

    /**
     * Indirizzo di Keycloak, iniettato a build time da `local.properties`
     * (chiave `keycloak.base.url`, default `http://localhost:8090`).
     *
     * Attenzione: l'host con cui l'app raggiunge Keycloak finisce nel claim `iss` del token,
     * perche' Keycloak costruisce l'issuer a partire dall'URL della richiesta. Il backend
     * valida `iss` contro `OAUTH2_ISSUER_URI`: i due valori devono descrivere lo stesso
     * indirizzo, altrimenti il login riesce e poi ogni chiamata all'API risponde 401.
     * Il `docker-compose.yml` fissa l'issuer con `KC_HOSTNAME_URL` proprio per questo.
     */
    private val KEYCLOAK_BASE =
        BuildConfig.KEYCLOAK_BASE_URL.trimEnd('/') + "/realms/travelapp"

    /**
     * Deve coincidere ESATTAMENTE con una delle "Valid redirect URIs" del client
     * `travelapp-android` sul realm, e con `appAuthRedirectScheme` nel `build.gradle.kts`.
     * Il confronto che fa Keycloak e' sulla stringa intera: uno schema diverso fa fallire
     * il login con "Invalid parameter: redirect_uri", prima ancora della form di login.
     */
    private const val REDIRECT_URI = "com.example.travelapp:/oauth2redirect"

    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("$KEYCLOAK_BASE/protocol/openid-connect/auth"),
        Uri.parse("$KEYCLOAK_BASE/protocol/openid-connect/token")
    )

    // Configurazione che usa il nostro builder HTTP invece del default HTTPS
    private val appAuthConfig = AppAuthConfiguration.Builder()
        .setConnectionBuilder(LocalConnectionBuilder)
        .build()

    fun creaIntentLogin(context: Context): Intent {
        val authService = AuthorizationService(context, appAuthConfig)

        val request = AuthorizationRequest.Builder(
            serviceConfig,
            "travelapp-android",
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        )
            .setScopes("openid", "profile", "email")
            .build()

        return authService.getAuthorizationRequestIntent(request)
    }

    fun scambiaCodicePToken(
        context: Context,
        intent: Intent,
        onSuccess: (accessToken: String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Usa appAuthConfig con LocalConnectionBuilder
        val authService = AuthorizationService(context, appAuthConfig)

        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        if (error != null) {
            onError("Errore login: ${error.message}")
            return
        }

        if (response == null) {
            onError("Risposta Keycloak vuota")
            return
        }

        authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, ex ->
            if (ex != null) {
                onError("Errore token: ${ex.message}")
                return@performTokenRequest
            }
            val token = tokenResponse?.accessToken
            if (token != null) {
                onSuccess(token)
            } else {
                onError("Token non ricevuto")
            }
        }
    }

    fun estraiRuolo(accessToken: String): String {
        return try {
            val payload = accessToken.split(".")[1]
            val decoded = String(
                android.util.Base64.decode(
                    payload,
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
                )
            )
            if (decoded.contains("ORGANIZZATORE")) "ORGANIZZATORE" else "VIAGGIATORE"
        } catch (e: Exception) {
            "VIAGGIATORE"
        }
    }
}