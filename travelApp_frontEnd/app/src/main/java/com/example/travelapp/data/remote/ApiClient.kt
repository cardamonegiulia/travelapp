package com.example.travelapp.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // URL del backend
    // Emulatore Android → 10.0.2.2 punta al localhost del PC
    // Telefono fisico → usa l'IP del tuo PC sulla rete WiFi
    private const val BASE_URL = "http://10.0.2.2:8081/"

    private var retrofit: Retrofit? = null

    fun getClient(context: Context): Retrofit {
        if (retrofit == null) {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(InterceptorAutenticazione(context))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
}