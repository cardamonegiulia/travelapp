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

/**
 * Repository per la gestione delle prenotazioni.
 *
 * Centralizza:
 * - recupero delle prenotazioni dell'utente;
 * - dettaglio prenotazione;
 * - creazione e annullamento;
 * - recupero dei saldi per organizzatore e amministratore.
 */
class PrenotazioneRepository(
    private val api: PrenotazioneApi
) {

    suspend fun getMiePrenotazioni(): List<Prenotazione> {
        return api
            .getMiePrenotazioni()
            .content
            .map { it.toDomain() }
    }

    /** Viaggi in corso, futuri e prenotazioni cancellate. */
    suspend fun getPrenotazioniAttuali(): List<Prenotazione> {
        return api
            .getMiePrenotazioniAttuali()
            .content
            .map { it.toDomain() }
    }

    /** Viaggi gia' conclusi: la lista da cui si lascia una recensione. */
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

    /**
     * Partenze ancora da fare di un proprio itinerario.
     *
     * Il filtro sulle date lo applica il backend: qui non si scarta nulla.
     */
    suspend fun getPartenzeItinerario(
        itinerarioId: Long
    ): List<PartenzaOrganizzatore> {
        return api
            .getPartenzeItinerario(itinerarioId)
            .map { it.toDomain() }
    }

    /** Viaggiatori prenotati su una partenza. */
    suspend fun getPrenotatiPartenza(
        disponibilitaId: Long
    ): List<PrenotatoPartenza> {
        return api
            .getPrenotatiPartenza(disponibilitaId)
            .content
            .map { it.toPrenotatoPartenza() }
    }

    /**
     * Elimina una partenza del proprio itinerario.
     *
     * Il rifiuto piu' probabile e' il 409 su una partenza gia' venduta: il motivo lo
     * spiega il backend, quindi si usa il suo messaggio invece di inventarne uno.
     */
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

    /**
     * Saldo complessivo della piattaforma.
     *
     * Destinato alle funzionalità amministrative.
     */
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

    /**
     * Saldo relativo all'organizzatore autenticato.
     */
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