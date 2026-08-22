package com.example.travelapp.data.remote

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Aggiunge l'header Authorization: Bearer <token>
 * alle richieste quando è disponibile un token salvato.
 */
class InterceptorAutenticazione(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val token = runBlocking {
            TokenManager.getToken(context).first()
        }

        val richiesta = chain.request()

        if (token.isNullOrBlank()) {
            return chain.proceed(richiesta)
        }

        val richiestaAutenticata = richiesta
            .newBuilder()
            .header(
                "Authorization",
                "Bearer $token"
            )
            .build()

        return chain.proceed(richiestaAutenticata)
    }
}