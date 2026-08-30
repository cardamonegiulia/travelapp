package com.example.travelapp.data.remote

import android.util.Base64
import org.json.JSONObject

/**
 * Dati dell'utente letti direttamente dall'access token.
 *
 * Servono a mostrare subito nome, email e ruolo giusti, senza aspettare la
 * chiamata a /api/utenti/me: il token è già sul dispositivo e questi claim
 * arrivano da Keycloak, quindi sono i dati veri dell'utente collegato.
 */
data class DatiToken(
    val nome: String,
    val cognome: String,
    val email: String,
    val ruolo: String
) {
    val nomeCompleto: String
        get() = listOf(nome, cognome)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

/**
 * Lettura dei claim di un JWT.
 *
 * Il token non viene verificato: la firma la controlla il backend a ogni
 * richiesta. Qui serve solo a leggere dati che l'utente ha già ottenuto
 * facendo il login.
 */
object LettoreToken {

    private const val RUOLO_PREDEFINITO = "VIAGGIATORE"

    /**
     * Ruoli applicativi in ordine di precedenza: un utente che ha più ruoli
     * viene trattato come il più alto tra quelli che possiede.
     */
    private val RUOLI_NOTI = listOf(
        "ADMIN",
        "ORGANIZZATORE",
        "VIAGGIATORE"
    )

    fun leggi(accessToken: String): DatiToken? {

        val claim = payload(accessToken) ?: return null

        val nomeCompleto = claim.optString("name")

        val nome = claim.optString("given_name")
            .takeIf { it.isNotBlank() }
            ?: nomeCompleto.substringBefore(" ")

        val cognome = claim.optString("family_name")
            .takeIf { it.isNotBlank() }
            ?: nomeCompleto.substringAfter(" ", "")

        val email = claim.optString("email")
            .takeIf { it.isNotBlank() }
            ?: claim.optString("preferred_username")

        return DatiToken(
            nome = nome.trim(),
            cognome = cognome.trim(),
            email = email.trim(),
            ruolo = ruoloDaClaim(claim)
        )
    }

    /**
     * Ruolo applicativo dell'utente, letto da realm_access.roles.
     */
    fun ruolo(accessToken: String): String {
        val claim = payload(accessToken) ?: return RUOLO_PREDEFINITO
        return ruoloDaClaim(claim)
    }

    private fun ruoloDaClaim(claim: JSONObject): String {

        val ruoli = claim
            .optJSONObject("realm_access")
            ?.optJSONArray("roles")
            ?: return RUOLO_PREDEFINITO

        val posseduti = buildSet {
            for (i in 0 until ruoli.length()) {
                add(ruoli.optString(i).uppercase())
            }
        }

        return RUOLI_NOTI.firstOrNull { it in posseduti }
            ?: RUOLO_PREDEFINITO
    }

    private fun payload(accessToken: String): JSONObject? {

        return try {

            val parti = accessToken.split(".")

            if (parti.size < 2) {
                return null
            }

            val decodificato = Base64.decode(
                parti[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            JSONObject(String(decodificato, Charsets.UTF_8))

        } catch (e: Exception) {
            null
        }
    }
}
