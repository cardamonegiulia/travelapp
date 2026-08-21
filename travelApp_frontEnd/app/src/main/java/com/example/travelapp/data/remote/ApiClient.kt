package com.example.travelapp.data.remote

import com.example.travelapp.data.remote.api.UtenteApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Configurazione unica del client HTTP: base url, timeout, interceptor del token. */
object ApiClient {

    /**
     * Backend di sviluppo. `10.0.2.2` è l'indirizzo con cui l'emulatore Android raggiunge
     * il `localhost` della macchina che lo ospita: `127.0.0.1`, dentro l'emulatore, è
     * l'emulatore stesso.
     *
     * La porta è quella di `mvnw spring-boot:run` (`server.port=8081`). Con
     * `docker compose up` il backend sta invece sulla 8080: è l'unico valore da cambiare.
     * Su dispositivo fisico va messo l'IP della macchina sulla rete locale, e l'host va
     * aggiunto a `res/xml/network_security_config.xml`.
     */
    const val BASE_URL: String = "http://10.0.2.2:8081/"

    /**
     * Client condiviso. OkHttp è pensato per essere istanziato una volta sola: ogni
     * istanza porta con sé il proprio pool di connessioni e i propri thread, e crearne una
     * per chiamata annulla il riuso delle connessioni.
     *
     * Serve anche fuori da Retrofit: il contenuto delle immagini sta dietro
     * autenticazione (`GET /api/immagini/{id}/contenuto`), quindi va scaricato con questo
     * client e non con una `URL.openStream()` qualsiasi, che non porterebbe il token.
     */
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(InterceptorAutenticazione())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            // l'upload di una foto può arrivare a 5 MB: il tempo di scrittura è più lungo
            // di quello di una richiesta JSON
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

    val utenteApi: UtenteApi by lazy { retrofit.create(UtenteApi::class.java) }

    /** Trasforma in url assoluto i link relativi che il backend mette nei DTO (`/api/...`). */
    fun urlAssoluto(percorso: String): String =
        if (percorso.startsWith("http://") || percorso.startsWith("https://")) percorso
        else BASE_URL.trimEnd('/') + "/" + percorso.trimStart('/')
}
