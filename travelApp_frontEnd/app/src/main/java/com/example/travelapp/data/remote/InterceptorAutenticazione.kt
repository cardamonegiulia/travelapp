package com.example.travelapp.data.remote

import android.content.Context
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

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
