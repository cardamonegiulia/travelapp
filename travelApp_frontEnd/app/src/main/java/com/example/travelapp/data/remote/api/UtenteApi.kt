package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.AggiornaTemaDto
import com.example.travelapp.data.remote.dto.CambioPasswordDto
import com.example.travelapp.data.remote.dto.PageResponse
import com.example.travelapp.data.remote.dto.UtenteResponseDto
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

interface UtenteApi {

    @POST("api/utenti/me")
    suspend fun sincronizzaProfilo(): Response<UtenteResponseDto>

    @POST("api/utenti/me/password")
    suspend fun cambiaPassword(@Body richiesta: CambioPasswordDto): Response<Unit>

    @PUT("api/utenti/{id}")
    suspend fun aggiornaTema(@Path("id") id: Long, @Body richiesta: AggiornaTemaDto): Response<UtenteResponseDto>

    @Multipart
    @PUT("api/utenti/me/foto-profilo")
    suspend fun impostaFotoProfilo(@Part file: MultipartBody.Part): Response<UtenteResponseDto>

    @DELETE("api/utenti/me/foto-profilo")
    suspend fun rimuoviFotoProfilo(): Response<Unit>

    @GET("api/utenti")
    suspend fun getTuttiGliUtenti(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<UtenteResponseDto>>

    @PUT("api/utenti/{id}/ruolo/admin")
    suspend fun promuoviAdAdmin(@Path("id") id: Long): Response<UtenteResponseDto>

    @DELETE("api/utenti/{id}")
    suspend fun eliminaUtente(@Path("id") id: Long): Response<Unit>
}