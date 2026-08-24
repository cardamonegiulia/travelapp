package com.example.travelapp.data.remote.api

import com.example.travelapp.data.remote.dto.RegistrazioneRequest
import com.example.travelapp.data.remote.dto.UtenteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    // Endpoint pubblico — non richiede token
    @POST("api/auth/registrazione")
    suspend fun registra(@Body richiesta: RegistrazioneRequest): Response<UtenteResponse>
}