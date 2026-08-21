package com.example.travelapp.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Estensione per creare il DataStore una sola volta
val Context.dataStore by preferencesDataStore(name = "auth_prefs")

object TokenManager {

    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val RUOLO = stringPreferencesKey("ruolo")

    // Salva il token JWT e il ruolo dell'utente
    suspend fun salvaToken(context: Context, token: String, ruolo: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = token
            prefs[RUOLO] = ruolo
        }
    }

    // Legge il token JWT
    fun getToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN] }

    // Legge il ruolo dell'utente
    fun getRuolo(context: Context): Flow<String?> =
        context.dataStore.data.map { it[RUOLO] }

    // Cancella token e ruolo al logout
    suspend fun cancellaToken(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}