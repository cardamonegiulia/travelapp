package com.example.travelapp.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "auth_prefs")

object TokenManager {

    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val RUOLO = stringPreferencesKey("ruolo")
    private val NOME = stringPreferencesKey("nome")
    private val EMAIL = stringPreferencesKey("email")

    suspend fun salvaToken(
        context: Context,
        token: String,
        ruolo: String,
        nome: String = "",
        email: String = ""
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = token
            prefs[RUOLO] = ruolo
            if (nome.isNotBlank()) prefs[NOME] = nome
            if (email.isNotBlank()) prefs[EMAIL] = email
        }
    }

    fun getToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN] }

    fun getRuolo(context: Context): Flow<String?> =
        context.dataStore.data.map { it[RUOLO] }

    fun getNome(context: Context): Flow<String?> =
        context.dataStore.data.map { it[NOME] }

    fun getEmail(context: Context): Flow<String?> =
        context.dataStore.data.map { it[EMAIL] }

    suspend fun cancellaToken(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}