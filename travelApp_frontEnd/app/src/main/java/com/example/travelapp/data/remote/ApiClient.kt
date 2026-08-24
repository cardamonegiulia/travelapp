package com.example.travelapp.data.remote

import android.content.Context
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.remote.api.ItinerarioApi
import com.example.travelapp.data.remote.api.PagamentoApi
import com.example.travelapp.data.remote.api.PrenotazioneApi
import com.example.travelapp.data.remote.api.SingolaAttivitaApi
import com.example.travelapp.data.remote.api.UtenteApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Configurazione centralizzata del client HTTP.
 *
 * Gestisce:
 * - base URL del backend, letto dalla configurazione locale a build time;
 * - timeout;
 * - API REST autenticate tramite JWT.
 */
object ApiClient {

    /**
     * Indirizzo del backend iniettato a build time.
     *
     * In questo modo ogni sviluppatore può usare il proprio indirizzo
     * senza modificare e committare il sorgente.
     */
    val BASE_URL: String = BuildConfig.BACKEND_BASE_URL

    @Volatile
    private var httpClientAutenticato: OkHttpClient? = null

    @Volatile
    private var retrofitAutenticato: Retrofit? = null

    /**
     * Client OkHttp condiviso che aggiunge il Bearer token
     * alle richieste verso il backend.
     */
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

    /**
     * Istanza Retrofit autenticata condivisa.
     */
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

    /**
     * Compatibilità con le classi che usano ancora ApiClient.getClient(context).
     * Restituisce comunque il Retrofit autenticato condiviso.
     */
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

    /**
     * Gli endpoint /api/utenti/me identificano l'utente
     * tramite JWT, quindi anche UtenteApi deve usare
     * necessariamente il client autenticato.
     */
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

    /**
     * Trasforma in URL assoluto i link relativi
     * restituiti dal backend.
     */
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