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

object KeycloakManager {

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
                "$KEYCLOAK_BASE/protocol/openid-connect/token"
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
                "travelapp-android",
                ResponseTypeValues.CODE,
                Uri.parse(REDIRECT_URI)
            )
                .setScopes(
                    "openid",
                    "profile",
                    "email"
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
        onSuccess: (accessToken: String) -> Unit,
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
                onSuccess(token)
            } else {
                onError(
                    "Token non ricevuto"
                )
            }
        }
    }

    /**
     * Ricava il ruolo applicativo dal JWT.
     *
     * Se non trova ORGANIZZATORE,
     * usa VIAGGIATORE come fallback.
     */
    fun estraiRuolo(
        accessToken: String
    ): String {

        return try {

            val payload =
                accessToken.split(".")[1]

            val decoded =
                String(
                    android.util.Base64.decode(
                        payload,
                        android.util.Base64.URL_SAFE or
                                android.util.Base64.NO_PADDING
                    )
                )

            if (
                decoded.contains(
                    "ORGANIZZATORE"
                )
            ) {
                "ORGANIZZATORE"
            } else {
                "VIAGGIATORE"
            }

        } catch (e: Exception) {
            "VIAGGIATORE"
        }
    }
}