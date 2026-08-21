package com.example.travelapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Aggiunge l'header `Authorization: Bearer <token>` a ogni chiamata protetta.
 *
 * Il token viene letto a ogni richiesta e non catturato alla costruzione: dopo un login o
 * un rinnovo, le chiamate successive usano subito quello nuovo senza dover ricreare il
 * client HTTP.
 *
 * Se il token manca la richiesta parte comunque, senza header: la risposta sarà un 401 del
 * backend, che è l'informazione corretta da mostrare ("devi accedere"). Bloccarla qui
 * darebbe un errore di rete generico, indistinguibile da un backend spento.
 */
class InterceptorAutenticazione(
    private val token: () -> String? = { SessioneUtente.accessToken }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val richiesta = chain.request()
        val valore = token()

        if (valore.isNullOrBlank()) {
            return chain.proceed(richiesta)
        }

        return chain.proceed(
            richiesta.newBuilder()
                .header("Authorization", "Bearer $valore")
                .build()
        )
    }
}
