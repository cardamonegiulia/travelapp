package com.example.travelapp.data.repository

import com.example.travelapp.data.remote.api.RecensioneApi
import com.example.travelapp.data.remote.dto.AggiornaRecensioneDto
import com.example.travelapp.data.remote.dto.CreaRecensioneDto
import com.example.travelapp.domain.model.Recensione
import org.json.JSONObject

class RecensioneRepository(
    private val api: RecensioneApi
) {

    suspend fun getRecensioniItinerario(itinerarioId: Long): Result<List<Recensione>> =
        try {
            val response = api.getRecensioniItinerario(itinerarioId)
            val corpo = response.body()
            if (response.isSuccessful && corpo != null) {
                Result.success(corpo.content.map { it.toDomain() })
            } else {
                Result.failure(Exception("Errore recupero recensioni: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * La propria recensione su una prenotazione.
     *
     * Il 204 non e' un errore: significa "viaggio non ancora recensito", ed e' proprio il
     * caso in cui il form si apre vuoto.
     */
    suspend fun getRecensionePrenotazione(prenotazioneId: Long): Result<Recensione?> =
        try {
            val response = api.getRecensionePrenotazione(prenotazioneId)
            when {
                response.code() == 204 -> Result.success(null)
                response.isSuccessful -> Result.success(response.body()?.toDomain())
                else -> Result.failure(Exception("Errore recupero recensione: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun creaRecensione(
        prenotazioneId: Long,
        votazione: Int,
        commento: String?
    ): Result<Recensione> =
        try {
            val response = api.creaRecensione(
                CreaRecensioneDto(
                    prenotazioneId = prenotazioneId,
                    votazione = votazione,
                    comm = commento
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                )
            )

            val corpo = response.body()

            if (response.isSuccessful && corpo != null) {
                Result.success(corpo.toDomain())
            } else {
                Result.failure(
                    Exception(
                        messaggioErrore(
                            response.code(),
                            response.errorBody()?.string()
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun aggiornaRecensione(
        recensioneId: Long,
        votazione: Int,
        commento: String?
    ): Result<Recensione> =
        try {
            val response = api.aggiornaRecensione(
                recensioneId,
                AggiornaRecensioneDto(
                    votazione = votazione,
                    comm = commento
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                )
            )

            val corpo = response.body()

            if (response.isSuccessful && corpo != null) {
                Result.success(corpo.toDomain())
            } else {
                Result.failure(
                    Exception(
                        messaggioErrore(
                            response.code(),
                            response.errorBody()?.string()
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    // I codici che il backend usa per le regole della recensione, tradotti in messaggi
    // comprensibili: il corpo ProblemDetail non e' pensato per essere mostrato cosi' com'e'.
    private fun messaggioErrore(
        codice: Int,
        corpoErrore: String?
    ): String {

        val dettaglioBackend =
            runCatching {
                corpoErrore
                    ?.takeIf { it.isNotBlank() }
                    ?.let { json ->
                        JSONObject(json)
                            .optString("detail")
                            .takeIf { it.isNotBlank() }
                    }
            }.getOrNull()

        if (dettaglioBackend != null) {
            return dettaglioBackend
        }

        return when (codice) {
            403 ->
                "Puoi recensire solo i viaggi che hai prenotato tu"

            404 ->
                "Prenotazione non trovata"

            409 ->
                "La recensione non può essere salvata per lo stato attuale del viaggio"

            else ->
                "Errore durante il salvataggio della recensione: HTTP $codice"
        }
    }
}
