package com.example.travelapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.travelapp.data.remote.ApiClient

// Punto di ingresso dell'applicazione: inizializzazioni globali (client HTTP, sessione).
class TravelApp : Application(), ImageLoaderFactory {

    /**
     * Coil di suo userebbe un OkHttpClient tutto suo, senza Authorization.
     *
     * Le immagini pero' stanno dietro /api/immagini/{id}/contenuto, che richiede
     * un utente autenticato: senza Bearer token ogni copertina tornerebbe 401 e
     * resterebbe il riquadro grigio. Riusando il client dell'app le richieste
     * passano dall'InterceptorAutenticazione come tutte le altre, rinnovo del
     * token compreso.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .callFactory { ApiClient.getHttpClient(this) }
            .crossfade(true)
            .build()
}
