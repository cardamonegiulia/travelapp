package com.example.travelapp.data.repository

import com.example.travelapp.data.remote.api.PrenotazioneApi
import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.data.remote.dto.toDomain
import com.example.travelapp.data.remote.dto.toPrenotatoPartenza
import com.example.travelapp.domain.model.PartenzaOrganizzatore
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.domain.model.PrenotatoPartenza
import org.json.JSONObject
import java.math.BigDecimal

class PrenotazioneRepository(
    private val api: PrenotazioneApi
) {

    suspend fun getMiePrenotazioni(): List<Prenotazione> {
        return api
            .getMiePrenotazioni()
            .content
            .map { it.toDomain() }
    }

    suspend fun getPrenotazioniAttuali(): List<Prenotazione> {
        return api
            .getMiePrenotazioniAttuali()
            .content
            .map { it.toDomain() }
    }
    suspend fun getViaggiConclusi(): List<Prenotazione> {
        return api
            .getMieiViaggiConclusi()
            .content
            .map { it.toDomain() }
    }

    suspend fun getPrenotazione(
        id: Long
    ): Prenotazione {
        return api
            .getPrenotazione(id)
            .toDomain()
    }

    suspend fun creaPrenotazione(
        request: CreaPrenotazioneDto
    ): Prenotazione {
        return api
            .creaPrenotazione(request)
            .toDomain()
    }

    suspend fun annullaPrenotazione(
        id: Long
    ): Prenotazione {
        return api
            .annullaPrenotazione(id)
            .toDomain()
    }

    suspend fun getPartenzeItinerario(
        itinerarioId: Long
    ): List<PartenzaOrganizzatore> {
        return api
            .getPartenzeItinerario(itinerarioId)
            .map { it.toDomain() }
    }
    suspend fun getPrenotatiPartenza(
        disponibilitaId: Long
    ): List<PrenotatoPartenza> {
        return api
            .getPrenotatiPartenza(disponibilitaId)
            .content
            .map { it.toPrenotatoPartenza() }
    }

    suspend fun eliminaPartenza(
        disponibilitaId: Long
    ): Result<Unit> =
        try {
            val response = api.eliminaPartenza(disponibilitaId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(
                        messaggioErroreEliminazione(
                            response.code(),
                            response.errorBody()?.string()
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun messaggioErroreEliminazione(
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
            403 -> "Non puoi eliminare le partenze di questo itinerario"
            404 -> "Partenza non trovata"
            409 -> "La partenza ha gia' delle prenotazioni e non puo' essere eliminata"
            else -> "Errore durante l'eliminazione della partenza: HTTP $codice"
        }
    }

    suspend fun getSaldoTotaleGlobale(): Result<BigDecimal> =
        try {
            val response =
                api.getSaldoTotaleGlobale()

            val corpo =
                response.body()

            if (
                response.isSuccessful &&
                corpo != null
            ) {
                Result.success(corpo)
            } else {
                Result.failure(
                    Exception(
                        "Errore recupero saldo globale: " +
                                "HTTP ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun getSaldoOrganizzatore(): Result<BigDecimal> =
        try {
            val response =
                api.getSaldoOrganizzatore()

            val corpo =
                response.body()

            if (
                response.isSuccessful &&
                corpo != null
            ) {
                Result.success(corpo)
            } else {
                Result.failure(
                    Exception(
                        "Errore recupero saldo organizzatore: " +
                                "HTTP ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
}