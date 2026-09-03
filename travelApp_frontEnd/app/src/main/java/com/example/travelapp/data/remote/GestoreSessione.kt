package com.example.travelapp.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object GestoreSessione {

    private val lucchettoRinnovo = Mutex()

    suspend fun tokenValido(context: Context): String? {

        val tokenSalvato = TokenManager.leggiAccessToken(context)

        if (tokenSalvato.isNullOrBlank()) {
            return null
        }

        if (!TokenManager.accessTokenScaduto(context)) {
            return tokenSalvato
        }

        return lucchettoRinnovo.withLock {

            val tokenAggiornato = TokenManager.leggiAccessToken(context)

            if (
                !tokenAggiornato.isNullOrBlank() &&
                !TokenManager.accessTokenScaduto(context)
            ) {
                return@withLock tokenAggiornato
            }

            val refreshToken =
                TokenManager.leggiRefreshToken(context)
                    ?: return@withLock null

            val esito = withContext(Dispatchers.IO) {
                KeycloakManager.rinnovaAccessToken(refreshToken)
            }

            when (esito) {

                is EsitoRinnovo.Successo -> {

                    TokenManager.aggiornaToken(
                        context = context,
                        accessToken = esito.sessione.accessToken,
                        refreshToken = esito.sessione.refreshToken,
                        scadenzaMs = esito.sessione.scadenzaMs
                    )

                    esito.sessione.accessToken
                }

                EsitoRinnovo.Rifiutato -> {
                    TokenManager.cancellaToken(context)
                    null
                }

                EsitoRinnovo.ErroreRete -> {
                    null
                }
            }
        }
    }

    suspend fun forzaRinnovo(context: Context): String? {

        return lucchettoRinnovo.withLock {

            val refreshToken =
                TokenManager.leggiRefreshToken(context)
                    ?: return@withLock null

            val esito = withContext(Dispatchers.IO) {
                KeycloakManager.rinnovaAccessToken(refreshToken)
            }

            when (esito) {

                is EsitoRinnovo.Successo -> {

                    TokenManager.aggiornaToken(
                        context = context,
                        accessToken = esito.sessione.accessToken,
                        refreshToken = esito.sessione.refreshToken,
                        scadenzaMs = esito.sessione.scadenzaMs
                    )

                    esito.sessione.accessToken
                }

                EsitoRinnovo.Rifiutato -> {
                    TokenManager.cancellaToken(context)
                    null
                }

                EsitoRinnovo.ErroreRete -> null
            }
        }
    }

    suspend fun datiUtenteDalToken(context: Context): DatiToken? {

        val token = TokenManager.leggiAccessToken(context)
            ?: return null

        return LettoreToken.leggi(token)
    }

    suspend fun logout(context: Context) {

        val refreshToken = TokenManager.leggiRefreshToken(context)

        if (!refreshToken.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                KeycloakManager.revocaSessione(refreshToken)
            }
        }

        TokenManager.cancellaToken(context)
    }

    suspend fun sessioneRipristinabile(context: Context): Boolean {

        if (TokenManager.leggiAccessToken(context).isNullOrBlank()) {
            return false
        }

        if (tokenValido(context) != null) {
            return true
        }

        return !TokenManager.leggiRefreshToken(context).isNullOrBlank()
    }
}
