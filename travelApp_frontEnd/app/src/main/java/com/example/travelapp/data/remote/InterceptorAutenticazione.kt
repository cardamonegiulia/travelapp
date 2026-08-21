package com.example.travelapp.data.remote

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class InterceptorAutenticazione(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Legge il token salvato nel DataStore
        val token = runBlocking {
            TokenManager.getToken(context).first()
        }

        val request = if (token != null) {
            // Aggiunge il Bearer Token a ogni richiesta
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}