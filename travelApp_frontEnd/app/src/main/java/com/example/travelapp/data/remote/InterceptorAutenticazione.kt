package com.example.travelapp.data.remote

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Aggiunge l'header Authorization: Bearer <token>
 * alle richieste protette quando è disponibile un token salvato.
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
            TokenManager.getToken(context).first()
        }

        if (token.isNullOrBlank()) {
            return chain.proceed(richiestaOriginale)
        }

        val richiestaAutenticata = richiestaOriginale
            .newBuilder()
            .header(
                "Authorization",
                "Bearer $token"
            )
            .build()

        return chain.proceed(richiestaAutenticata)
    }
}