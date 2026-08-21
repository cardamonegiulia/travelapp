package com.example.travelapp.data.remote

import android.content.Context
import android.content.Intent
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import android.net.Uri

object KeycloakManager {

    // URL del tuo Keycloak
    private const val KEYCLOAK_BASE = "http://10.0.2.2:8090/realms/travelapp"

    // Questo schema deve corrispondere a quello nel AndroidManifest.xml
    private const val REDIRECT_URI = "com.example.travelapp:/callback"

    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("$KEYCLOAK_BASE/protocol/openid-connect/auth"),   // endpoint login
        Uri.parse("$KEYCLOAK_BASE/protocol/openid-connect/token")   // endpoint token
    )

    /**
     * Costruisce l'Intent che apre il browser con la pagina di login Keycloak.
     * Il ruolo passato viene usato solo per la registrazione — per il login
     * Keycloak sa già chi sei dalle credenziali.
     */
    fun creaIntentLogin(context: Context): Intent {
        val authService = AuthorizationService(context)

        val request = AuthorizationRequest.Builder(
            serviceConfig,
            "travelapp-backend",        // client_id configurato su Keycloak
            ResponseTypeValues.CODE,     // Authorization Code Flow
            Uri.parse(REDIRECT_URI)
        )
            .setScopes("openid", "profile", "email")
            .build()

        return authService.getAuthorizationRequestIntent(request)
    }

    /**
     * Scambia il codice ricevuto da Keycloak con il token JWT reale.
     * Chiamato dopo che l'utente ha fatto login nel browser.
     */
    fun scambiaCodicePToken(
        context: Context,
        intent: Intent,
        onSuccess: (accessToken: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val authService = AuthorizationService(context)
        val response = net.openid.appauth.AuthorizationResponse.fromIntent(intent)
        val error = net.openid.appauth.AuthorizationException.fromIntent(intent)

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

    /**
     * Legge il ruolo dell'utente dal token JWT.
     * Keycloak mette i ruoli nel campo realm_access.roles del token.
     */
    fun estraiRuolo(accessToken: String): String {
        return try {
            // Il token JWT è diviso in 3 parti separate da "."
            // La parte centrale (index 1) contiene i dati utente in Base64
            val payload = accessToken.split(".")[1]
            val decoded = String(android.util.Base64.decode(
                payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
            ))
            // Cerchiamo il ruolo ORGANIZZATORE — altrimenti è VIAGGIATORE
            if (decoded.contains("ORGANIZZATORE")) "ORGANIZZATORE" else "VIAGGIATORE"
        } catch (e: Exception) {
            "VIAGGIATORE" // default in caso di errore
        }
    }
}