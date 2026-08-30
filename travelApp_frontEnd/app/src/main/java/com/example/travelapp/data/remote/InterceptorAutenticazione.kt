package com.example.travelapp.data.remote

import android.content.Context
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Aggiunge l'header Authorization: Bearer <token>
 * alle richieste protette quando è disponibile un token salvato.
 *
 * Se il token è scaduto viene rinnovato con il refresh token prima di
 * inviare la richiesta; se il backend risponde comunque 401 si tenta un
 * ultimo rinnovo e si ripete la chiamata una sola volta.
 *
 * Le rotte pubbliche vengono lasciate senza Authorization.
 */
class InterceptorAutenticazione(
    private val context: Context
) : Interceptor {

    private val percorsiPubblici = listOf(
        "/api/auth/registrazione"
    )

    override fun intercept(chain: Interceptor.Chain): Response {

        val richiestaOriginale = chain.request()

        val eRottaPubblica = percorsiPubblici.any { percorso ->
            richiestaOriginale.url.encodedPath.endsWith(percorso)
        }

        if (eRottaPubblica) {
            return chain.proceed(richiestaOriginale)
        }

        val token = runBlocking {
            GestoreSessione.tokenValido(context)
        }

        if (token.isNullOrBlank()) {
            return chain.proceed(richiestaOriginale)
        }

        val risposta = chain.proceed(
            conAutorizzazione(richiestaOriginale, token)
        )

        if (risposta.code != 401) {
            return risposta
        }

        // Il token è stato invalidato prima della scadenza prevista:
        // proviamo a rinnovarlo e a ripetere la richiesta una volta sola.
        val tokenRinnovato = runBlocking {
            GestoreSessione.forzaRinnovo(context)
        }

        if (tokenRinnovato.isNullOrBlank()) {
            return risposta
        }

        risposta.close()

        return chain.proceed(
            conAutorizzazione(richiestaOriginale, tokenRinnovato)
        )
    }

    private fun conAutorizzazione(
        richiesta: Request,
        token: String
    ): Request =
        richiesta
            .newBuilder()
            .header(
                "Authorization",
                "Bearer $token"
            )
            .build()
}
