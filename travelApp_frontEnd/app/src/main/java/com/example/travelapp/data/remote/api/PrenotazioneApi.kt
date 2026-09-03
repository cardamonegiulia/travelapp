package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.data.remote.dto.PageDto
import com.example.travelapp.data.remote.dto.PartenzaOrganizzatoreDto
import com.example.travelapp.data.remote.dto.PrenotazioneDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.math.BigDecimal

interface PrenotazioneApi {
    @GET("api/prenotazioni/saldo/totale")
    suspend fun getSaldoTotaleGlobale(): Response<BigDecimal>

    @GET("api/prenotazioni/saldo/organizzatore")
    suspend fun getSaldoOrganizzatore(): Response<BigDecimal>

    @GET("api/prenotazioni/mie")
    suspend fun getMiePrenotazioni(): PageDto<PrenotazioneDto>

    @GET("api/prenotazioni/mie/attuali")
    suspend fun getMiePrenotazioniAttuali(): PageDto<PrenotazioneDto>
    @GET("api/prenotazioni/mie/concluse")
    suspend fun getMieiViaggiConclusi(): PageDto<PrenotazioneDto>
    @GET("api/prenotazioni/organizzatore/itinerari/{itinerarioId}/partenze")
    suspend fun getPartenzeItinerario(
        @Path("itinerarioId") itinerarioId: Long
    ): List<PartenzaOrganizzatoreDto>
    @GET("api/prenotazioni/organizzatore/partenze/{disponibilitaId}")
    suspend fun getPrenotatiPartenza(
        @Path("disponibilitaId") disponibilitaId: Long
    ): PageDto<PrenotazioneDto>
    @DELETE("api/prenotazioni/organizzatore/partenze/{disponibilitaId}")
    suspend fun eliminaPartenza(
        @Path("disponibilitaId") disponibilitaId: Long
    ): Response<Unit>

    @GET("api/prenotazioni/{id}")
    suspend fun getPrenotazione(
        @Path("id") id: Long
    ): PrenotazioneDto
    @POST("api/prenotazioni")
    suspend fun creaPrenotazione(
        @Body request: CreaPrenotazioneDto
    ): PrenotazioneDto
    @POST("api/prenotazioni/{id}/annulla")
    suspend fun annullaPrenotazione(
        @Path("id") id: Long
    ): PrenotazioneDto
}