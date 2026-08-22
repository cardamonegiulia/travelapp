package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.UtenteResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

/** Endpoint del contesto identity usati dal profilo. */
interface UtenteApi {

    /**
     * Profilo dell'utente del token, creandone il record locale se è il primo accesso.
     *
     * È una POST e non una GET perché può scrivere (provisioning just-in-time), ed è
     * l'unica rotta da cui il client ricava il proprio profilo: non esiste un
     * `GET /api/utenti/me`, e `GET /api/utenti/{id}` richiede di conoscere già l'id.
     */
    @POST("api/utenti/me")
    suspend fun sincronizzaProfilo(): Response<UtenteResponseDto>

    /**
     * Imposta o sostituisce la foto profilo. Il nome del campo — `file` — e quello del
     * file devono rispettare quanto si aspetta il backend: l'estensione dichiarata deve
     * essere `.jpg`/`.jpeg`/`.png` **e** coincidere col contenuto reale, che il server
     * riconosce dai primi byte.
     *
     * Restituisce il profilo aggiornato, così basta una chiamata sola per mostrare il
     * nuovo avatar.
     */
    @Multipart
    @PUT("api/utenti/me/foto-profilo")
    suspend fun impostaFotoProfilo(@Part file: MultipartBody.Part): Response<UtenteResponseDto>

    @DELETE("api/utenti/me/foto-profilo")
    suspend fun rimuoviFotoProfilo(): Response<Unit>
}
