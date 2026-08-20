package com.example.travelapp.data.remote

import com.example.travelapp.data.remote.api.PrenotazioneApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.travelapp.data.remote.api.PagamentoApi

// Configurazione unica del client HTTP: base url, timeout, interceptor del token.
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val prenotazioneApi: PrenotazioneApi =
        retrofit.create(PrenotazioneApi::class.java)

    val pagamentoApi: PagamentoApi =
        retrofit.create(PagamentoApi::class.java)
}