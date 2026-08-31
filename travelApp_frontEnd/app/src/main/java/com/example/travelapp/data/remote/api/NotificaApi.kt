package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.NotificaDto
import com.example.travelapp.data.remote.dto.PageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificaApi {

    @GET("api/notifiche")
    suspend fun getMieNotifiche(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<PageDto<NotificaDto>>

    @GET("api/notifiche/non-lette")
    suspend fun contaNonLette(): Response<Long>

    @POST("api/notifiche/{id}/letta")
    suspend fun segnaLetta(
        @Path("id") id: Long
    ): Response<NotificaDto>
}
