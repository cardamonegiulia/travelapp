package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.data.remote.dto.PageDto
import com.example.travelapp.data.remote.dto.PrenotazioneDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PrenotazioneApi {
    @GET("api/prenotazioni/mie")
    suspend fun getMiePrenotazioni(): PageDto<PrenotazioneDto>

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
