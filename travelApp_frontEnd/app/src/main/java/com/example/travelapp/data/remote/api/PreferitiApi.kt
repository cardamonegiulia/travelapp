package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.CondivisioneRequestDto
import com.example.travelapp.data.remote.dto.ListaPreferitiRequestDto
import com.example.travelapp.data.remote.dto.ListaPreferitiResponseDto
import com.example.travelapp.data.remote.dto.PreferitoItinerarioRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PreferitiApi {

    @GET("api/preferiti")
    suspend fun getMieListe(): Response<List<ListaPreferitiResponseDto>>

    @GET("api/preferiti/condivise-con-me")
    suspend fun getListeCondiviseConMe(): Response<List<ListaPreferitiResponseDto>>

    @GET("api/preferiti/{listaId}")
    suspend fun getLista(
        @Path("listaId") listaId: Long
    ): Response<ListaPreferitiResponseDto>

    @POST("api/preferiti")
    suspend fun creaLista(
        @Body request: ListaPreferitiRequestDto
    ): Response<ListaPreferitiResponseDto>

    @PUT("api/preferiti/{listaId}")
    suspend fun aggiornaLista(
        @Path("listaId") listaId: Long,
        @Body request: ListaPreferitiRequestDto
    ): Response<ListaPreferitiResponseDto>

    @DELETE("api/preferiti/{listaId}")
    suspend fun eliminaLista(
        @Path("listaId") listaId: Long
    ): Response<Unit>

    @POST("api/preferiti/{listaId}/itinerari")
    suspend fun aggiungiItinerario(
        @Path("listaId") listaId: Long,
        @Body request: PreferitoItinerarioRequestDto
    ): Response<ListaPreferitiResponseDto>

    @DELETE("api/preferiti/{listaId}/itinerari/{itinerarioId}")
    suspend fun rimuoviItinerario(
        @Path("listaId") listaId: Long,
        @Path("itinerarioId") itinerarioId: Long
    ): Response<ListaPreferitiResponseDto>

    @POST("api/preferiti/itinerari")
    suspend fun aggiungiAiPreferiti(
        @Body request: PreferitoItinerarioRequestDto
    ): Response<ListaPreferitiResponseDto>

    @DELETE("api/preferiti/itinerari/{itinerarioId}")
    suspend fun rimuoviDaiPreferiti(
        @Path("itinerarioId") itinerarioId: Long
    ): Response<Unit>

    @POST("api/preferiti/{listaId}/condivisioni")
    suspend fun condividi(
        @Path("listaId") listaId: Long,
        @Body request: CondivisioneRequestDto
    ): Response<ListaPreferitiResponseDto>

    @DELETE("api/preferiti/{listaId}/condivisioni/{utenteId}")
    suspend fun revocaCondivisione(
        @Path("listaId") listaId: Long,
        @Path("utenteId") utenteId: Long
    ): Response<ListaPreferitiResponseDto>
}
