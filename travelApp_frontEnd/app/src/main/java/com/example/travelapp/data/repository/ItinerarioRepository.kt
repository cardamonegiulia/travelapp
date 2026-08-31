package com.example.travelapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.travelapp.data.remote.CorpoImmagine
import com.example.travelapp.data.remote.ImmagineNonCaricabile
import com.example.travelapp.data.remote.api.ItinerarioApi
import com.example.travelapp.data.remote.dto.AttivitaExtraResponseDto
import com.example.travelapp.data.remote.dto.DisponibilitaItinerarioResponseDto
import com.example.travelapp.data.remote.dto.ItinerarioRequestDto
import com.example.travelapp.domain.model.ImmagineResponse
import com.example.travelapp.domain.model.Itinerario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ItinerarioRepository(
    private val api: ItinerarioApi
) {

    suspend fun getAllItinerari(page: Int = 0, size: Int = 20): Result<List<Itinerario>> {
        return try {
            val response = api.getAllItinerari(page, size)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.content.map { it.toDomain() })
            } else {
                Result.failure(Exception("Errore recupero itinerari: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItinerarioById(id: Long): Result<Itinerario> {
        return try {
            val response = api.getItinerarioById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Itinerario non trovato: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDisponibilitaItinerario(id: Long): Result<List<DisponibilitaItinerarioResponseDto>> {
        return try {
            val response = api.getDisponibilitaByItinerario(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Errore recupero disponibilità: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttivitaExtra(
        id: Long
    ): Result<List<AttivitaExtraResponseDto>> {
        return try {
            val response = api.getAttivitaExtra(id)

            if (
                response.isSuccessful &&
                response.body() != null
            ) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception(
                        "Errore recupero extra: HTTP ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun createItinerario(request: ItinerarioRequestDto): Result<Itinerario> {
        return try {
            val response = api.createItinerario(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Errore creazione itinerario: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateItinerario(id: Long, request: ItinerarioRequestDto): Result<Itinerario> {
        return try {
            val response = api.updateItinerario(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Errore modifica itinerario: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItinerario(id: Long): Result<Unit> {
        return try {
            val response = api.deleteItinerario(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore eliminazione itinerario: HTTP ${response.code()}"))
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
                Result.failure(Exception("Errore upload immagine itinerario: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}