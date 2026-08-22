package com.example.travelapp.data.remote

import android.content.Context
import com.example.travelapp.data.remote.api.PagamentoApi
import com.example.travelapp.data.remote.api.PrenotazioneApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Emulatore Android:
    // 10.0.2.2 punta al localhost del PC.
    //
    // Telefono fisico:
    // servirà l'IP locale del PC sulla stessa rete WiFi.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private var retrofit: Retrofit? = null

    fun getClient(context: Context): Retrofit {

        if (retrofit == null) {

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(
                    InterceptorAutenticazione(context.applicationContext)
                )
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!
    }

    fun getPrenotazioneApi(context: Context): PrenotazioneApi {
        return getClient(context)
            .create(PrenotazioneApi::class.java)
    }

    fun getPagamentoApi(context: Context): PagamentoApi {
        return getClient(context)
            .create(PagamentoApi::class.java)
    }
}