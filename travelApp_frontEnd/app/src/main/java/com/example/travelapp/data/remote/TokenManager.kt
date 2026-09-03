package com.example.travelapp.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "auth_prefs")

object TokenManager {

    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val SCADENZA_ACCESS_TOKEN = longPreferencesKey("scadenza_access_token")
    private val RUOLO = stringPreferencesKey("ruolo")

    private const val MARGINE_SCADENZA_MS = 30_000L

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

    fun getToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN] }

    fun getRuolo(context: Context): Flow<String?> =
        context.dataStore.data.map { it[RUOLO] }

    suspend fun leggiAccessToken(context: Context): String? =
        context.dataStore.data.first()[ACCESS_TOKEN]

    suspend fun leggiRefreshToken(context: Context): String? =
        context.dataStore.data.first()[REFRESH_TOKEN]

    suspend fun leggiRuolo(context: Context): String? =
        context.dataStore.data.first()[RUOLO]

    suspend fun accessTokenScaduto(context: Context): Boolean {
        val scadenza = context.dataStore.data.first()[SCADENZA_ACCESS_TOKEN] ?: 0L
        return System.currentTimeMillis() >= (scadenza - MARGINE_SCADENZA_MS)
    }

    suspend fun cancellaToken(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}
