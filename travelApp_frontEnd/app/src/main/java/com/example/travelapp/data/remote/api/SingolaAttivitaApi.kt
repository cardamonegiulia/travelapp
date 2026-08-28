package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.PageResponse
import com.example.travelapp.data.remote.dto.SessioneAttivitaResponseDto
import com.example.travelapp.data.remote.dto.SingolaAttivitaRequestDto
import com.example.travelapp.data.remote.dto.SingolaAttivitaResponseDto
import com.example.travelapp.domain.model.ImmagineResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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
        @Query("inizio") inizio: String,
        @Query("fine") fine: String,
        @Query("giorni") giorni: List<Int>
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

    @Multipart
    @POST("api/attivita/{id}/immagini")
    suspend fun caricaImmagine(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part
    ): Response<ImmagineResponse>

    @GET("api/attivita/{id}/immagini")
    suspend fun getImmaginiAttivita(
        @Path("id") id: Long
    ): Response<List<ImmagineResponse>>

    @DELETE("api/attivita/{id}/immagini/{immagineId}")
    suspend fun eliminaImmagine(
        @Path("id") id: Long,
        @Path("immagineId") immagineId: Long
    ): Response<Unit>
}