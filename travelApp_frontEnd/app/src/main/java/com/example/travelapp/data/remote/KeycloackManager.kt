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
     * In questo modo ogni sviluppatore può usare localhost, emulator,
     * adb reverse o IP LAN senza modificare il sorgente condiviso.
     */
    private val KEYCLOAK_BASE =
        BuildConfig.KEYCLOAK_BASE_URL
            .trimEnd('/') +
                "/realms/travelapp"

    /**
     * Deve coincidere con una Valid Redirect URI
     * configurata sul client travelapp-android.
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
     * Permette ad AppAuth di comunicare con Keycloak
     * anche durante lo sviluppo locale via HTTP.
     */
    private val appAuthConfig =
        AppAuthConfiguration.Builder()
            .setConnectionBuilder(
                LocalConnectionBuilder
            )
            .build()

    /**
     * Crea l'intent per avviare il login Keycloak.
     *
     * loginHint è opzionale e permette di precompilare
     * username/email nella schermata Keycloak.
     */
    fun creaIntentLogin(
        context: Context,
        loginHint: String? = null
    ): Intent {

        val authService =
            AuthorizationService(
                context,
                appAuthConfig
            )

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

        return authService
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

        val authService =
            AuthorizationService(
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
            onError(
                "Errore login: ${error.message}"
            )
            return
        }

        if (response == null) {
            onError(
                "Risposta Keycloak vuota"
            )
            return
        }

        authService.performTokenRequest(
            response.createTokenExchangeRequest()
        ) { tokenResponse, ex ->

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
     * In assenza di un ruolo riconosciuto usa VIAGGIATORE
     * come fallback.
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