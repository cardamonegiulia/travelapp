package com.example.travelapp.data.repository

import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.api.PrenotazioneApi
import java.math.BigDecimal

class PrenotazioneRepository(
    private val api: PrenotazioneApi = ApiClient.prenotazioneApi
) {

    suspend fun getSaldoTotaleGlobale(): Result<BigDecimal> = try {
        val response = api.getSaldoTotaleGlobale()
        val corpo = response.body()
        if (response.isSuccessful && corpo != null) {
            Result.success(corpo)
        } else {
            Result.failure(Exception("Errore recupero saldo globale: HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSaldoOrganizzatore(): Result<BigDecimal> = try {
        val response = api.getSaldoOrganizzatore()
        val corpo = response.body()
        if (response.isSuccessful && corpo != null) {
            Result.success(corpo)
        } else {
            Result.failure(Exception("Errore recupero saldo organizzatore: HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}