package com.example.travelapp.data.remote

import com.example.travelapp.data.remote.api.ItinerarioApi
import com.example.travelapp.data.remote.api.PrenotazioneApi
import com.example.travelapp.data.remote.api.SingolaAttivitaApi
import com.example.travelapp.data.remote.api.UtenteApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Configurazione unica del client HTTP: base url, timeout, interceptor del token.
object ApiClient {

    // 10.0.2.2 punta al localhost del Mac quando usi l'emulatore Android
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
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

    val prenotazioneApi: PrenotazioneApi by lazy {
        retrofit.create(PrenotazioneApi::class.java)
    }
}