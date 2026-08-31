package com.example.travelapp.data.repository

import com.example.travelapp.data.remote.api.NotificaApi
import com.example.travelapp.domain.model.Notifica

class NotificaRepository(
    private val api: NotificaApi
) {

    suspend fun getMieNotifiche(): Result<List<Notifica>> =
        try {
            val response = api.getMieNotifiche()
            val corpo = response.body()
            if (response.isSuccessful && corpo != null) {
                Result.success(corpo.content.map { it.toDomain() })
            } else {
                Result.failure(Exception("Errore recupero notifiche: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun contaNonLette(): Result<Long> =
        try {
            val response = api.contaNonLette()
            val corpo = response.body()
            if (response.isSuccessful && corpo != null) {
                Result.success(corpo)
            } else {
                Result.failure(Exception("Errore conteggio notifiche: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun segnaLetta(id: Long): Result<Unit> =
        try {
            val response = api.segnaLetta(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore aggiornamento notifica: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
