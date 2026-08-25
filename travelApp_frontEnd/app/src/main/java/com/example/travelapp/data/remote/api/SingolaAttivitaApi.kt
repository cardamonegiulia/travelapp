package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.PageResponse
import com.example.travelapp.data.remote.dto.SessioneAttivitaResponseDto
import com.example.travelapp.data.remote.dto.SingolaAttivitaRequestDto
import com.example.travelapp.data.remote.dto.SingolaAttivitaResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SingolaAttivitaApi {

    @GET("api/attivita")
    suspend fun getAllAttivita(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "id,desc"
    ): Response<PageResponse<SingolaAttivitaResponseDto>>

    @GET("api/attivita/{id}")
    suspend fun getAttivitaById(
        @Path("id") id: Long
    ): Response<SingolaAttivitaResponseDto>

    @GET("api/attivita/{id}/sessioni")
    suspend fun getSessioniByAttivita(
        @Path("id") id: Long
    ): Response<List<SessioneAttivitaResponseDto>>

    @POST("api/attivita/con-sessioni")
    suspend fun createAttivitaConSessioni(
        @Body request: SingolaAttivitaRequestDto,
        @Query("inizio") inizio: String, // Formato "YYYY-MM-DD"
        @Query("fine") fine: String,     // Formato "YYYY-MM-DD"
        @Query("giorni") giorni: List<Int> // 1 (Lunedì) .. 7 (Domenica)
    ): Response<SingolaAttivitaResponseDto>

    @PUT("api/attivita/{id}")
    suspend fun updateAttivita(
        @Path("id") id: Long,
        @Body request: SingolaAttivitaRequestDto
    ): Response<SingolaAttivitaResponseDto>

    @DELETE("api/attivita/{id}")
    suspend fun deleteAttivita(
        @Path("id") id: Long
    ): Response<Unit>
}