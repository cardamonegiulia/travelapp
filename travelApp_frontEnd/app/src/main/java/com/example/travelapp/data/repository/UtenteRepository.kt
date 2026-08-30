package com.example.travelapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.CorpoImmagine
import com.example.travelapp.data.remote.ImmagineNonCaricabile
import com.example.travelapp.data.remote.api.UtenteApi
import com.example.travelapp.data.remote.dto.AggiornaTemaDto
import com.example.travelapp.data.remote.dto.CambioPasswordDto
import com.example.travelapp.domain.model.Utente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class UtenteRepository(
    context: Context,
    private val api: UtenteApi =  ApiClient.getUtenteApi(context)
) {
    private val context: Context = context.applicationContext

    suspend fun caricaProfilo(): Result<Utente> = chiamata("Errore nel recupero del profilo") {
        api.sincronizzaProfilo()
    }

    suspend fun impostaFotoProfilo(uri: Uri): Result<Utente> {
        val parte = try {
            // decodifica, ridimensionamento e ricompressione sono lavoro di CPU su qualche
            // megabyte: il chiamante è il viewModelScope, cioè il thread principale
            withContext(Dispatchers.IO) { CorpoImmagine.da(context, uri) }
        } catch (e: ImmagineNonCaricabile) {
            // file illeggibile o non decodificabile: è un errore da mostrare all'utente, non
            // un guasto, e non vale la pena disturbare il backend per farselo dire
            return Result.failure(e)
        }
        return chiamata("Errore nel caricamento della foto") { api.impostaFotoProfilo(parte) }
    }

    // Richiede un login recente (401 altrimenti); se va a buon fine chiude tutte le sessioni.
    suspend fun cambiaPassword(nuovaPassword: String): Result<Unit> =
        try {
            val risposta = api.cambiaPassword(CambioPasswordDto(nuovaPassword))
            if (risposta.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(messaggioErroreCambioPassword(risposta)))
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun messaggioErroreCambioPassword(risposta: Response<*>): String = when (risposta.code()) {
        400 -> "La password non rispetta i requisiti richiesti"
        401 -> "Per cambiare la password devi aver effettuato l'accesso di recente: rifai il login"
        503 -> "Servizio di autenticazione non disponibile, riprova più tardi"
        else -> "Cambio password non riuscito: HTTP ${risposta.code()}"
    }

    suspend fun aggiornaTema(id: Long, temaScuro: Boolean): Result<Utente> =
        chiamata("Errore nel salvataggio del tema") {
            api.aggiornaTema(id, AggiornaTemaDto(if (temaScuro) "SCURO" else "CHIARO"))
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