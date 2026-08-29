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
import org.json.JSONObject

object KeycloakManager {

    private val KEYCLOAK_BASE =
        BuildConfig.KEYCLOAK_BASE_URL
            .trimEnd('/') +
                "/realms/travelapp"

    private const val REDIRECT_URI =
        "com.example.travelapp:/oauth2redirect"

    private val serviceConfig =
        AuthorizationServiceConfiguration(
            Uri.parse("$KEYCLOAK_BASE/protocol/openid-connect/auth"),
            Uri.parse("$KEYCLOAK_BASE/protocol/openid-connect/token")
        )

    private val appAuthConfig =
        AppAuthConfiguration.Builder()
            .setConnectionBuilder(LocalConnectionBuilder)
            .build()

    private var authService: AuthorizationService? = null

    fun creaIntentLogin(
        context: Context,
        loginHint: String? = null,
        forzaLogin: Boolean = false
    ): Intent {
        authService?.dispose()

        val servizio = AuthorizationService(context, appAuthConfig)
        authService = servizio

        val requestBuilder =
            AuthorizationRequest.Builder(
                serviceConfig,
                "travelapp-android",
                ResponseTypeValues.CODE,
                Uri.parse(REDIRECT_URI)
            ).setScopes("openid", "profile", "email", "roles")

        if (!loginHint.isNullOrBlank()) {
            requestBuilder.setLoginHint(loginHint)
        }

        if (forzaLogin) {
            requestBuilder.setPrompt("login")
        }

        return servizio.getAuthorizationRequestIntent(requestBuilder.build())
    }

    fun scambiaCodicePToken(
        context: Context,
        intent: Intent,
        onSuccess: (accessToken: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val servizio = authService ?: AuthorizationService(context, appAuthConfig)
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        if (error != null) {
            servizio.dispose()
            authService = null
            onError("Errore login: ${error.message}")
            return
        }

        if (response == null) {
            servizio.dispose()
            authService = null
            onError("Risposta Keycloak vuota")
            return
        }

        servizio.performTokenRequest(
            response.createTokenExchangeRequest()
        ) { tokenResponse, ex ->
            servizio.dispose()
            authService = null

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

    fun decodificaPayloadJwt(accessToken: String): JSONObject? {
        return try {
            val parti = accessToken.split(".")
            if (parti.size < 2) return null
            val decoded = String(
                android.util.Base64.decode(
                    parti[1],
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
                )
            )
            JSONObject(decoded)
        } catch (e: Exception) {
            null
        }
    }

    fun estraiRuolo(accessToken: String): String {
        val json = decodificaPayloadJwt(accessToken) ?: return "VIAGGIATORE"
        val raw = json.toString()
        return when {
            raw.contains("ADMIN") -> "ADMIN"
            raw.contains("ORGANIZZATORE") -> "ORGANIZZATORE"
            else -> "VIAGGIATORE"
        }
    }

    fun estraiNome(accessToken: String): String {
        val json = decodificaPayloadJwt(accessToken) ?: return ""
        val name = json.optString("name", "")
        if (name.isNotBlank()) return name

        val givenName = json.optString("given_name", "")
        val familyName = json.optString("family_name", "")
        val completo = listOf(givenName, familyName).filter { it.isNotBlank() }.joinToString(" ")
        if (completo.isNotBlank()) return completo

        val preferred = json.optString("preferred_username", "")
        if (preferred.isNotBlank()) return preferred

        val email = json.optString("email", "")
        if (email.contains("@")) return email.substringBefore("@")

        return ""
    }

    fun estraiEmail(accessToken: String): String {
        val json = decodificaPayloadJwt(accessToken) ?: return ""
        val email = json.optString("email", "")
        if (email.isNotBlank()) return email
        return json.optString("preferred_username", "")
    }
}