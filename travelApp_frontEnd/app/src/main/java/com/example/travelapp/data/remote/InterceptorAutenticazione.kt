package com.example.travelapp.data.remote

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class InterceptorAutenticazione(private val context: Context) : Interceptor {

    private val percorsiPubblici = listOf("/api/auth/registrazione")

    override fun intercept(chain: Interceptor.Chain): Response {
        val richiestaOriginale = chain.request()
        val eRottaPubblica = percorsiPubblici.any { richiestaOriginale.url().encodedPath().endsWith(it) }

        if (eRottaPubblica) {
            return chain.proceed(richiestaOriginale)
        }

        // Legge il token salvato nel DataStore
        val token = runBlocking {
            TokenManager.getToken(context).first()
        }

        val request = if (token != null) {
            // Aggiunge il Bearer Token a ogni richiesta
            richiestaOriginale.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            richiestaOriginale
        }

        return chain.proceed(request)
    }
}