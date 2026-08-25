package com.example.travelapp.data.repository

import com.example.travelapp.data.remote.api.PrenotazioneApi
import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.data.remote.dto.toDomain
import com.example.travelapp.domain.model.Prenotazione
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