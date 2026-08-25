package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.DisponibilitaItinerarioResponseDto
import com.example.travelapp.data.remote.dto.ItinerarioRequestDto
import com.example.travelapp.data.remote.dto.ItinerarioResponseDto
import com.example.travelapp.data.remote.dto.PageResponse
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

interface ItinerarioApi {

    @GET("api/itinerari")
    suspend fun getAllItinerari(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "id,desc"
    ): Response<PageResponse<ItinerarioResponseDto>>

    @GET("api/itinerari/{id}")
    suspend fun getItinerarioById(
        @Path("id") id: Long
    ): Response<ItinerarioResponseDto>

    @GET("api/itinerari/{id}/disponibilita")
    suspend fun getDisponibilitaByItinerario(
        @Path("id") id: Long
    ): Response<List<DisponibilitaItinerarioResponseDto>>

    @POST("api/itinerari")
    suspend fun createItinerario(
        @Body request: ItinerarioRequestDto
    ): Response<ItinerarioResponseDto>

    @PUT("api/itinerari/{id}")
    suspend fun updateItinerario(
        @Path("id") id: Long,
        @Body request: ItinerarioRequestDto
    ): Response<ItinerarioResponseDto>

    @DELETE("api/itinerari/{id}")
    suspend fun deleteItinerario(
        @Path("id") id: Long
    ): Response<Unit>

    @GET("api/itinerari/{id}/immagini")
    suspend fun getImmaginiItinerario(
        @Path("id") id: Long
    ): Response<List<ImmagineResponse>>

    @Multipart
    @POST("api/itinerari/{id}/immagini")
    suspend fun caricaImmagine(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part
    ): Response<ImmagineResponse>

    @DELETE("api/itinerari/{id}/immagini/{immagineId}")
    suspend fun eliminaImmagine(
        @Path("id") id: Long,
        @Path("immagineId") immagineId: Long
    ): Response<Unit>
}