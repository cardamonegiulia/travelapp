package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.PageDto
import com.example.travelapp.data.remote.dto.PagamentoDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PagamentoApi {
    @POST("api/pagamenti/prenotazioni/{prenotazioneId}/paga")
    suspend fun pagaPrenotazione(
        @Path("prenotazioneId") prenotazioneId: Long
    ): PagamentoDto

    @GET("api/pagamenti/miei")
    suspend fun getMieiPagamenti(): PageDto<PagamentoDto>
}