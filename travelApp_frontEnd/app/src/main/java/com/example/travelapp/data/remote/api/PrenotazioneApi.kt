package com.example.travelapp.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import java.math.BigDecimal

interface PrenotazioneApi {

    @GET("api/prenotazioni/saldo/totale")
    suspend fun getSaldoTotaleGlobale(): Response<BigDecimal>

    @GET("api/prenotazioni/saldo/organizzatore")
    suspend fun getSaldoOrganizzatore(): Response<BigDecimal>
}