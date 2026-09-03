package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.AggiornaRecensioneDto
import com.example.travelapp.data.remote.dto.CreaRecensioneDto
import com.example.travelapp.data.remote.dto.PageDto
import com.example.travelapp.data.remote.dto.RecensioneResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RecensioneApi {

    @GET("api/itinerari/{id}/recensioni")
    suspend fun getRecensioniItinerario(
        @Path("id") itinerarioId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageDto<RecensioneResponseDto>>

    @GET("api/recensioni/mie")
    suspend fun getMieRecensioni(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageDto<RecensioneResponseDto>>

    @GET("api/recensioni/prenotazione/{prenotazioneId}")
    suspend fun getRecensionePrenotazione(
        @Path("prenotazioneId") prenotazioneId: Long
    ): Response<RecensioneResponseDto>

    @POST("api/recensioni")
    suspend fun creaRecensione(
        @Body request: CreaRecensioneDto
    ): Response<RecensioneResponseDto>

    @PUT("api/recensioni/{id}")
    suspend fun aggiornaRecensione(
        @Path("id") id: Long,
        @Body request: AggiornaRecensioneDto
    ): Response<RecensioneResponseDto>
}
