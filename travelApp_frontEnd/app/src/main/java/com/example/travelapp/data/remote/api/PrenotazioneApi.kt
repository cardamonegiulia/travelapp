package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.data.remote.dto.PageDto
import com.example.travelapp.data.remote.dto.PrenotazioneDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.math.BigDecimal

interface PrenotazioneApi {

    /*
     * Saldo complessivo della piattaforma.
     * Utilizzato dalle funzionalità amministrative.
     */
    @GET("api/prenotazioni/saldo/totale")
    suspend fun getSaldoTotaleGlobale(): Response<BigDecimal>

    /*
     * Saldo relativo all'organizzatore autenticato.
     */
    @GET("api/prenotazioni/saldo/organizzatore")
    suspend fun getSaldoOrganizzatore(): Response<BigDecimal>

    /*
     * Prenotazioni dell'utente autenticato.
     */
    @GET("api/prenotazioni/mie")
    suspend fun getMiePrenotazioni(): PageDto<PrenotazioneDto>

    /*
     * Dettaglio di una singola prenotazione.
     */
    @GET("api/prenotazioni/{id}")
    suspend fun getPrenotazione(
        @Path("id") id: Long
    ): PrenotazioneDto

    /*
     * Creazione di una nuova prenotazione.
     */
    @POST("api/prenotazioni")
    suspend fun creaPrenotazione(
        @Body request: CreaPrenotazioneDto
    ): PrenotazioneDto

    /*
     * Annullamento di una prenotazione esistente.
     */
    @POST("api/prenotazioni/{id}/annulla")
    suspend fun annullaPrenotazione(
        @Path("id") id: Long
    ): PrenotazioneDto
}