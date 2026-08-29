package com.example.travelapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.CorpoImmagine
import com.example.travelapp.data.remote.ImmagineNonCaricabile
import com.example.travelapp.data.remote.api.UtenteApi
import com.example.travelapp.domain.model.Utente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class UtenteRepository(
    context: Context,
    private val api: UtenteApi = ApiClient.getUtenteApi(context)
) {
    private val context: Context = context.applicationContext

    suspend fun caricaProfilo(): Result<Utente> = chiamata("Errore nel recupero del profilo") {
        api.sincronizzaProfilo()
    }

    suspend fun impostaFotoProfilo(uri: Uri): Result<Utente> {
        val parte = try {
            withContext(Dispatchers.IO) { CorpoImmagine.da(context, uri) }
        } catch (e: ImmagineNonCaricabile) {
            return Result.failure(e)
        }
        return chiamata("Errore nel caricamento della foto") { api.impostaFotoProfilo(parte) }
    }

    suspend fun rimuoviFotoProfilo(): Result<Unit> =
        try {
            val risposta = api.rimuoviFotoProfilo()
            if (risposta.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(messaggioErrore("Errore nella rimozione della foto", risposta)))
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun getTuttiGliUtenti(page: Int = 0, size: Int = 50): Result<List<Utente>> =
        try {
            val risposta = api.getTuttiGliUtenti(page, size)
            val corpo = risposta.body()
            if (risposta.isSuccessful && corpo != null) {
                Result.success(corpo.content.map { it.toDomain() })
            } else {
                Result.failure(Exception("Errore recupero utenti: HTTP ${risposta.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun promuoviAdAdmin(id: Long): Result<Utente> = chiamata("Errore promozione utente") {
        api.promuoviAdAdmin(id)
    }

    suspend fun eliminaUtente(id: Long): Result<Unit> =
        try {
            val risposta = api.eliminaUtente(id)
            if (risposta.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Errore eliminazione utente: HTTP ${risposta.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun chiamata(
        contesto: String,
        blocco: suspend () -> Response<com.example.travelapp.data.remote.dto.UtenteResponseDto>
    ): Result<Utente> =
        try {
            val risposta = blocco()
            val corpo = risposta.body()
            if (risposta.isSuccessful && corpo != null) {
                Result.success(corpo.toDomain())
            } else {
                Result.failure(Exception(messaggioErrore(contesto, risposta)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun messaggioErrore(contesto: String, risposta: Response<*>): String = when (risposta.code()) {
        401 -> "Sessione scaduta: accedi di nuovo"
        403 -> "Non hai i permessi per questa operazione"
        413 -> "L'immagine è troppo grande"
        400 -> "$contesto: il file non è valido"
        else -> "$contesto: HTTP ${risposta.code()}"
    }
}