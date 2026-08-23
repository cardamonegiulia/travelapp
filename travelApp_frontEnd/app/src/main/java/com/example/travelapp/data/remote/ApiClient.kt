package com.example.travelapp.data.remote

import android.content.Context
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
 * - base URL del backend;
 * - timeout;
 * - API del catalogo/utente;
 * - API booking protette tramite interceptor di autenticazione.
 */
object ApiClient {

    /*
     * Emulatore Android:
     * 10.0.2.2 punta al localhost del PC.
     *
     * Telefono fisico:
     * servirà l'IP locale del PC sulla stessa rete.
     *
     * Verificare che il backend utilizzi effettivamente la porta 8081.
     */
    const val BASE_URL = "http://192.168.1.51:8081/"

    /*
     * Client HTTP generale.
     *
     * Manteniamo i timeout già introdotti dagli altri moduli.
     */
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /*
     * Retrofit generale già utilizzato dagli altri moduli.
     */
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

    /*
     * Retrofit autenticato.
     *
     * Viene creato quando abbiamo un Context perché
     * InterceptorAutenticazione necessita del Context
     * per recuperare il token.
     */
    private var retrofitAutenticato: Retrofit? = null

    fun getClientAutenticato(context: Context): Retrofit {

        if (retrofitAutenticato == null) {

            val clientAutenticato = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(
                    InterceptorAutenticazione(
                        context.applicationContext
                    )
                )
                .build()

            retrofitAutenticato = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientAutenticato)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofitAutenticato!!
    }

    fun getPrenotazioneApi(context: Context): PrenotazioneApi {
        return getClientAutenticato(context)
            .create(PrenotazioneApi::class.java)
    }

    fun getPagamentoApi(context: Context): PagamentoApi {
        return getClientAutenticato(context)
            .create(PagamentoApi::class.java)
    }

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

    /**
     * Restituisce l'API utente usando il client autenticato.
     *
     * Gli endpoint /api/utenti/me identificano l'utente tramite il JWT,
     * quindi devono ricevere l'header Authorization: Bearer <token>.
     * Usare il client Retrofit generale causerebbe il fallimento della
     * sincronizzazione del profilo dell'utente autenticato.
     * uso questo metodo in utenterepositorei dentro class
     */

    fun getUtenteApi(context: Context): UtenteApi{
        return getClientAutenticato(context).create(UtenteApi::class.java)
    }






}