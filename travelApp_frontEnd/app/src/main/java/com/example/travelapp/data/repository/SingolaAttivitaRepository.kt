package com.example.travelapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.travelapp.data.remote.CorpoImmagine
import com.example.travelapp.data.remote.ImmagineNonCaricabile
import com.example.travelapp.data.remote.api.SingolaAttivitaApi
import com.example.travelapp.data.remote.dto.SessioneAttivitaResponseDto
import com.example.travelapp.data.remote.dto.SingolaAttivitaRequestDto
import com.example.travelapp.domain.model.ImmagineResponse
import com.example.travelapp.domain.model.SingolaAttivita
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SingolaAttivitaRepository(
    private val api: SingolaAttivitaApi
) {

    suspend fun getAllAttivita(page: Int = 0, size: Int = 20): Result<List<SingolaAttivita>> {
        return try {
            val response = api.getAllAttivita(page, size)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.content.map { it.toDomain() })
            } else {
                Result.failure(Exception("Errore recupero attività: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttivitaById(id: Long): Result<SingolaAttivita> {
        return try {
            val response = api.getAttivitaById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Attività non trovata: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessioniAttivita(id: Long): Result<List<SessioneAttivitaResponseDto>> {
        return try {
            val response = api.getSessioniByAttivita(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Errore recupero sessioni: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAttivitaConSessioni(
        request: SingolaAttivitaRequestDto,
        inizio: String,
        fine: String,
        giorni: List<Int>
    ): Result<SingolaAttivita> {
        return try {
            val response = api.createAttivitaConSessioni(request, inizio, fine, giorni)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Errore creazione attività: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAttivita(id: Long, request: SingolaAttivitaRequestDto): Result<SingolaAttivita> {
        return try {
            val response = api.updateAttivita(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Errore modifica attività: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAttivita(id: Long): Result<Unit> {
        return try {
            val response = api.deleteAttivita(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore eliminazione attività: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun caricaImmagine(context: Context, id: Long, uri: Uri): Result<ImmagineResponse> {
        val parte = try {
            withContext(Dispatchers.IO) { CorpoImmagine.da(context, uri) }
        } catch (e: ImmagineNonCaricabile) {
            return Result.failure(e)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return try {
            val response = api.caricaImmagine(id, parte)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Errore upload immagine attività: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}