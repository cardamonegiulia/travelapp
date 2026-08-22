package com.example.travelapp.data.remote

import com.example.travelapp.data.remote.api.ItinerarioApi
import com.example.travelapp.data.remote.api.SingolaAttivitaApi
import com.example.travelapp.data.remote.api.UtenteApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Configurazione unica del client HTTP: base url, timeout, interceptor del token. */
object ApiClient {

    // 10.0.2.2 punta al localhost del Mac quando usi l'emulatore Android
    const val BASE_URL: String = "http://10.0.2.2:8081/"

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val itinerarioApi: ItinerarioApi by lazy {
        retrofit.create(ItinerarioApi::class.java)
    }

    val singolaAttivitaApi: SingolaAttivitaApi by lazy {
        retrofit.create(SingolaAttivitaApi::class.java)
    }

    val utenteApi: UtenteApi by lazy {
        retrofit.create(UtenteApi::class.java)
    }

    /** Trasforma in url assoluto i link relativi che il backend mette nei DTO (`/api/...`). */
    fun urlAssoluto(percorso: String): String =
        if (percorso.startsWith("http://") || percorso.startsWith("https://")) percorso
        else BASE_URL.trimEnd('/') + "/" + percorso.trimStart('/')
}