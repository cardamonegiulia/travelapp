package com.example.travelapp.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Punto unico da cui ottenere un access token utilizzabile.
 *
 * Se il token salvato è scaduto tenta il rinnovo con il refresh token,
 * così l'utente resta autenticato anche dopo aver chiuso e riaperto l'app.
 */
object GestoreSessione {

    /**
     * Evita che più richieste parallele rinnovino lo stesso token
     * contemporaneamente, invalidandolo a vicenda.
     */
    private val lucchettoRinnovo = Mutex()

    /**
     * Restituisce un access token valido, rinnovandolo se necessario.
     *
     * null significa che non c'è nessuna sessione utilizzabile:
     * la richiesta partirà senza Authorization e l'app tornerà al login.
     */
    suspend fun tokenValido(context: Context): String? {

        val tokenSalvato = TokenManager.leggiAccessToken(context)

        if (tokenSalvato.isNullOrBlank()) {
            return null
        }

        if (!TokenManager.accessTokenScaduto(context)) {
            return tokenSalvato
        }

        return lucchettoRinnovo.withLock {

            // Nel frattempo un'altra richiesta potrebbe aver già rinnovato.
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
                    // Sessione non più recuperabile: si riparte dal login.
                    TokenManager.cancellaToken(context)
                    null
                }

                EsitoRinnovo.ErroreRete -> {
                    // Problema temporaneo: teniamo la sessione e riproviamo dopo.
                    null
                }
            }
        }
    }

    /**
     * Rinnova comunque il token, anche se risulta non ancora scaduto.
     *
     * Serve quando il backend risponde 401: il token è stato invalidato
     * prima della sua scadenza nominale.
     */
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

    /**
     * Dati dell'utente collegato, letti dai claim del token salvato.
     *
     * Va bene anche un access token scaduto: l'identità di chi ha fatto il
     * login non cambia, e questo evita di aspettare la rete per mostrare
     * nome ed email giusti.
     */
    suspend fun datiUtenteDalToken(context: Context): DatiToken? {

        val token = TokenManager.leggiAccessToken(context)
            ?: return null

        return LettoreToken.leggi(token)
    }

    /**
     * Chiude la sessione: revoca i token su Keycloak e li rimuove dal
     * dispositivo.
     */
    suspend fun logout(context: Context) {

        val refreshToken = TokenManager.leggiRefreshToken(context)

        if (!refreshToken.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                KeycloakManager.revocaSessione(refreshToken)
            }
        }

        TokenManager.cancellaToken(context)
    }

    /**
     * Verifica all'avvio se esiste una sessione da ripristinare.
     *
     * Un errore di rete non deve far ripartire dal login: se il refresh
     * token è ancora salvato consideriamo la sessione viva e lasciamo che
     * sia la prima richiesta riuscita a rinnovare il token.
     */
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
