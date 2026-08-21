package com.example.travelapp.data.remote

/**
 * Access token dell'utente loggato, in memoria.
 *
 * È l'unico punto da cui [InterceptorAutenticazione] legge il token: il login lo scrive
 * qui e nessun altro pezzo dell'app deve conoscerlo.
 *
 * **Stato attuale: il token non viene mai scritto da nessuno.** Il login Authorization
 * Code + PKCE descritto in `docs/login-android-setup.md` non è ancora implementato, quindi
 * [accessToken] resta `null` e il backend risponde 401 a ogni chiamata sotto `/api`.
 * Quando quel flusso esisterà, l'unica riga da aggiungere è l'assegnazione qui sotto al
 * termine del login (e l'azzeramento al logout): il resto della catena è già a posto.
 *
 * Volutamente in memoria e non su disco: un access token dura pochi minuti e va perso al
 * riavvio, mentre è il *refresh* token a dover essere conservato — e per quello serve
 * `EncryptedSharedPreferences`, non un campo statico.
 */
object SessioneUtente {

    @Volatile
    var accessToken: String? = null

    val autenticato: Boolean
        get() = !accessToken.isNullOrBlank()

    fun pulisci() {
        accessToken = null
    }
}
