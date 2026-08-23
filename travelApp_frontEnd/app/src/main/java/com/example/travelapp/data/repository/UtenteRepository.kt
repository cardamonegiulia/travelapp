package com.example.travelapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.CorpoImmagine
import com.example.travelapp.data.remote.ImmagineNonCaricabile
import com.example.travelapp.data.remote.api.UtenteApi
import com.example.travelapp.domain.model.Utente
import retrofit2.Response

/** Profilo dell'utente corrente e aggiornamento dei suoi dati. */
class UtenteRepository(
    context: Context,
    private val api: UtenteApi = ApiClient.getUtenteApi(context)
) {

    // applicationContext: il repository può sopravvivere alla Activity che l'ha creato
    private val context: Context = context.applicationContext

    /** Profilo dell'utente loggato (lo crea in locale se è il primo accesso). */
    suspend fun caricaProfilo(): Result<Utente> = chiamata("Errore nel recupero del profilo") {
        api.sincronizzaProfilo()
    }

    /**
     * Carica [uri] come foto profilo e restituisce il profilo aggiornato, con l'url da cui
     * scaricare la nuova immagine.
     *
     * [uri] è quello che arriva dal photo picker di sistema: il permesso di leggerlo vale
     * per la sessione corrente, quindi il file va letto adesso e non conservato per dopo.
     */
    suspend fun impostaFotoProfilo(uri: Uri): Result<Utente> {
        val parte = try {
            CorpoImmagine.da(context, uri)
        } catch (e: ImmagineNonCaricabile) {
            // formato o dimensione sbagliati: è un errore da mostrare all'utente, non un
            // guasto, e non vale la pena disturbare il backend per farselo dire
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

    /**
     * Traduce i codici che l'utente può davvero incontrare. Sugli altri resta il codice
     * HTTP: inventare un messaggio rassicurante per un errore che non si conosce nasconde
     * il problema invece di risolverlo.
     */
    private fun messaggioErrore(contesto: String, risposta: Response<*>): String = when (risposta.code()) {
        401 -> "Sessione scaduta: accedi di nuovo"
        403 -> "Non hai i permessi per questa operazione"
        413 -> "L'immagine è troppo grande"
        400 -> "$contesto: il file non è un'immagine JPEG o PNG valida"
        else -> "$contesto: HTTP ${risposta.code()}"
    }
}
