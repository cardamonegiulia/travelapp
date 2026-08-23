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
 * - base URL del backend, letto da `local.properties` a build time;
 * - timeout;
 * - le API REST, tutte autenticate: sotto `/api` SecurityConfig protegge ogni rotta,
 *   quindi non esiste piu' una variante "senza token" da cui una chiamata possa passare
 *   per distrazione.
 */
object ApiClient {

    /**
     * Indirizzo del backend, iniettato a build time da `local.properties`
     * (chiave `backend.base.url`, default `http://localhost:8081/`).
     *
     * Non e' piu' una costante scritta nel sorgente: cambiava a ogni cambio di rete, e la
     * modifica finiva per essere committata o dimenticata. Come impostarlo, e come far
     * raggiungere il PC a un telefono fisico, e' spiegato in `local.properties.example`.
     */
    val BASE_URL: String = BuildConfig.BACKEND_BASE_URL

    /*
     * Client HTTP e Retrofit, in un'unica variante: quella che AGGIUNGE il token.
     *
     * Prima ne esistevano due, e le API di catalogo, itinerari e profilo passavano da
     * quella senza interceptor. Ma SecurityConfig protegge ogni rotta sotto `/api`, non solo
     * prenotazioni e pagamenti: quelle chiamate partivano senza header Authorization e
     * tornavano 401 anche a login perfettamente riuscito. Una sola strada, e il caso
     * "ho dimenticato di autenticare questa" non si ripresenta.
     *
     * L'interceptor lascia comunque partire la richiesta quando il token non c'e'
     * (vedi InterceptorAutenticazione): serve a distinguere "non ho fatto il login" —
     * 401 dal backend — da "il backend non risponde" — errore di connessione.
     *
     * Serve un Context perche' il token sta nel DataStore, che e' per applicazione.
     * Le istanze sono memoizzate: OkHttp vuole essere condiviso, ha un suo pool di
     * connessioni e un suo thread pool.
     */
    @Volatile
    private var httpClientAutenticato: OkHttpClient? = null

    @Volatile
    private var retrofitAutenticato: Retrofit? = null

    /** Client OkHttp che allega il bearer token: usarlo per QUALUNQUE richiesta a `/api`. */
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
            .also { httpClientAutenticato = it }

    @Synchronized
    fun getClientAutenticato(context: Context): Retrofit =
        retrofitAutenticato ?: Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .also { retrofitAutenticato = it }

    fun getItinerarioApi(context: Context): ItinerarioApi =
        getClientAutenticato(context).create(ItinerarioApi::class.java)

    fun getSingolaAttivitaApi(context: Context): SingolaAttivitaApi =
        getClientAutenticato(context).create(SingolaAttivitaApi::class.java)

    fun getUtenteApi(context: Context): UtenteApi =
        getClientAutenticato(context).create(UtenteApi::class.java)

    fun getPrenotazioneApi(context: Context): PrenotazioneApi =
        getClientAutenticato(context).create(PrenotazioneApi::class.java)

    fun getPagamentoApi(context: Context): PagamentoApi =
        getClientAutenticato(context).create(PagamentoApi::class.java)

    /**
     * Trasforma in URL assoluto i link relativi restituiti
     * dal backend, ad esempio "/api/...".
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