package com.example.travelapp.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Estensione per creare il DataStore una sola volta
val Context.dataStore by preferencesDataStore(name = "auth_prefs")

object TokenManager {

    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val SCADENZA_ACCESS_TOKEN = longPreferencesKey("scadenza_access_token")
    private val RUOLO = stringPreferencesKey("ruolo")

    /**
     * Consideriamo il token già scaduto qualche secondo prima della scadenza
     * reale, così una richiesta partita al limite non si trova con un token
     * morto a metà strada.
     */
    private const val MARGINE_SCADENZA_MS = 30_000L

    /**
     * Salva l'intera sessione ottenuta da Keycloak.
     *
     * scadenzaMs è l'istante (epoch millis) in cui l'access token smette
     * di essere valido; se Keycloak non lo comunica salviamo 0, che vale
     * come "scadenza sconosciuta" e forza un rinnovo al primo utilizzo.
     */
    suspend fun salvaSessione(
        context: Context,
        accessToken: String,
        refreshToken: String?,
        scadenzaMs: Long?,
        ruolo: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[SCADENZA_ACCESS_TOKEN] = scadenzaMs ?: 0L
            prefs[RUOLO] = ruolo

            if (!refreshToken.isNullOrBlank()) {
                prefs[REFRESH_TOKEN] = refreshToken
            }
        }
    }

    /**
     * Aggiorna solo i token dopo un rinnovo, lasciando intatto il ruolo.
     */
    suspend fun aggiornaToken(
        context: Context,
        accessToken: String,
        refreshToken: String?,
        scadenzaMs: Long?
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[SCADENZA_ACCESS_TOKEN] = scadenzaMs ?: 0L

            if (!refreshToken.isNullOrBlank()) {
                prefs[REFRESH_TOKEN] = refreshToken
            }
        }
    }

    // Legge il token JWT
    fun getToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN] }

    // Legge il ruolo dell'utente
    fun getRuolo(context: Context): Flow<String?> =
        context.dataStore.data.map { it[RUOLO] }

    suspend fun leggiAccessToken(context: Context): String? =
        context.dataStore.data.first()[ACCESS_TOKEN]

    suspend fun leggiRefreshToken(context: Context): String? =
        context.dataStore.data.first()[REFRESH_TOKEN]

    suspend fun leggiRuolo(context: Context): String? =
        context.dataStore.data.first()[RUOLO]

    /**
     * True quando l'access token salvato è scaduto (o sta per scadere).
     */
    suspend fun accessTokenScaduto(context: Context): Boolean {
        val scadenza = context.dataStore.data.first()[SCADENZA_ACCESS_TOKEN] ?: 0L
        return System.currentTimeMillis() >= (scadenza - MARGINE_SCADENZA_MS)
    }

    // Cancella token e ruolo al logout
    suspend fun cancellaToken(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}
