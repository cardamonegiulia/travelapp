package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.data.remote.dto.PageDto
import com.example.travelapp.data.remote.dto.PartenzaOrganizzatoreDto
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
     * Prenotazioni ancora da concludere: viaggi in corso, futuri e cancellati.
     */
    @GET("api/prenotazioni/mie/attuali")
    suspend fun getMiePrenotazioniAttuali(): PageDto<PrenotazioneDto>

    /*
     * Viaggi gia' conclusi: sono quelli che si possono recensire.
     */
    @GET("api/prenotazioni/mie/concluse")
    suspend fun getMieiViaggiConclusi(): PageDto<PrenotazioneDto>

    /*
     * Partenze ancora da fare di un proprio itinerario, con quanto hanno venduto.
     * Riservato all'organizzatore che l'ha creato.
     */
    @GET("api/prenotazioni/organizzatore/itinerari/{itinerarioId}/partenze")
    suspend fun getPartenzeItinerario(
        @Path("itinerarioId") itinerarioId: Long
    ): List<PartenzaOrganizzatoreDto>

    /*
     * Viaggiatori prenotati su una singola partenza.
     */
    @GET("api/prenotazioni/organizzatore/partenze/{disponibilitaId}")
    suspend fun getPrenotatiPartenza(
        @Path("disponibilitaId") disponibilitaId: Long
    ): PageDto<PrenotazioneDto>

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