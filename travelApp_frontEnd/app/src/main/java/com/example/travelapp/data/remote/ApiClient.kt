package com.example.travelapp.data.remote

import com.example.travelapp.data.remote.api.UtenteApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Configurazione unica del client HTTP: base url, timeout, interceptor del token. */
object ApiClient {

    /**
     * Backend di sviluppo, raggiunto da un telefono fisico: qui va l'IP della macchina di
     * sviluppo sulla rete locale, l'unico indirizzo che il telefono può comporre. Cambia
     * se il router ne assegna un altro via DHCP, e va tenuto allineato all'elenco di
     * `res/xml/network_security_config.xml`, che autorizza il traffico in chiaro host per
     * host.
     *
     * Sull'**emulatore** va invece usato `http://10.0.2.2:8081/`: `10.0.2.2` è l'alias con
     * cui l'emulatore raggiunge il `localhost` della macchina che lo ospita, perché
     * `127.0.0.1`, lì dentro, è l'emulatore stesso. Su un telefono fisico quell'indirizzo
     * non esiste e ogni chiamata fallisce con "failed to connect".
     *
     * La porta è quella di `mvnw spring-boot:run` (`server.port=8081`). Con
     * `docker compose up` il backend sta invece sulla 8080.
     */
    const val BASE_URL: String = "http://10.145.178.54:8081/"

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
