package com.example.travelapp.data.remote

import android.content.Context
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.remote.api.ItinerarioApi
import com.example.travelapp.data.remote.api.PagamentoApi
import com.example.travelapp.data.remote.api.PreferitiApi
import com.example.travelapp.data.remote.api.NotificaApi
import com.example.travelapp.data.remote.api.PrenotazioneApi
import com.example.travelapp.data.remote.api.RecensioneApi
import com.example.travelapp.data.remote.api.SingolaAttivitaApi
import com.example.travelapp.data.remote.api.UtenteApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ApiClient {

    val BASE_URL: String = BuildConfig.BACKEND_BASE_URL

    @Volatile
    private var httpClientAutenticato: OkHttpClient? = null

    @Volatile
    private var retrofitAutenticato: Retrofit? = null

    @Synchronized
    fun getHttpClient(context: Context): OkHttpClient =
        httpClientAutenticato ?: OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(
                InterceptorAutenticazione(
                    context.applicationContext
                )
            )
            .build()
            .also {
                httpClientAutenticato = it
            }

    @Synchronized
    fun getClientAutenticato(context: Context): Retrofit =
        retrofitAutenticato ?: Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getHttpClient(context))
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .also {
                retrofitAutenticato = it
            }

    fun getPreferitiApi(context: Context): PreferitiApi =
        getClientAutenticato(context).create(PreferitiApi::class.java)

    fun getClient(context: Context): Retrofit =
        getClientAutenticato(context)

    fun getItinerarioApi(
        context: Context
    ): ItinerarioApi =
        getClientAutenticato(context)
            .create(ItinerarioApi::class.java)

    fun getSingolaAttivitaApi(
        context: Context
    ): SingolaAttivitaApi =
        getClientAutenticato(context)
            .create(SingolaAttivitaApi::class.java)

    fun getUtenteApi(
        context: Context
    ): UtenteApi =
        getClientAutenticato(context)
            .create(UtenteApi::class.java)

    fun getPrenotazioneApi(
        context: Context
    ): PrenotazioneApi =
        getClientAutenticato(context)
            .create(PrenotazioneApi::class.java)

    fun getPagamentoApi(
        context: Context
    ): PagamentoApi =
        getClientAutenticato(context)
            .create(PagamentoApi::class.java)

    fun getRecensioneApi(
        context: Context
    ): RecensioneApi =
        getClientAutenticato(context)
            .create(RecensioneApi::class.java)

    fun getNotificaApi(
        context: Context
    ): NotificaApi =
        getClientAutenticato(context)
            .create(NotificaApi::class.java)

    fun urlAssoluto(percorso: String): String {
        return if (
            percorso.startsWith("http://") ||
            percorso.startsWith("https://")
        ) {
            percorso
        } else {
            BASE_URL.trimEnd('/') +
                    "/" +
                    percorso.trimStart('/')
        }
    }
}