package com.example.travelapp.data.repository

import com.example.travelapp.data.remote.api.PreferitiApi
import com.example.travelapp.data.remote.dto.CondivisioneRequestDto
import com.example.travelapp.data.remote.dto.ListaPreferitiRequestDto
import com.example.travelapp.data.remote.dto.ListaPreferitiResponseDto
import com.example.travelapp.data.remote.dto.PreferitoItinerarioRequestDto
import com.example.travelapp.domain.model.ListaPreferiti
import com.example.travelapp.domain.model.VisibilitaLista
import retrofit2.Response

class PreferitiRepository(
    private val api: PreferitiApi
) {

    suspend fun getMieListe(): Result<List<ListaPreferiti>> =
        elenco("Errore nel caricamento delle liste") { api.getMieListe() }

    suspend fun getListeCondiviseConMe(): Result<List<ListaPreferiti>> =
        elenco("Errore nel caricamento delle liste condivise") { api.getListeCondiviseConMe() }

    suspend fun getLista(listaId: Long): Result<ListaPreferiti> =
        lista("Lista non trovata") { api.getLista(listaId) }

    suspend fun creaLista(nome: String, visibilita: VisibilitaLista): Result<ListaPreferiti> =
        lista("Errore nella creazione della lista") {
            api.creaLista(ListaPreferitiRequestDto(nome, visibilita.name))
        }

    suspend fun aggiornaLista(
        listaId: Long,
        nome: String,
        visibilita: VisibilitaLista
    ): Result<ListaPreferiti> =
        lista("Errore nella modifica della lista") {
            api.aggiornaLista(listaId, ListaPreferitiRequestDto(nome, visibilita.name))
        }

    suspend fun eliminaLista(listaId: Long): Result<Unit> =
        vuoto("Errore nell'eliminazione della lista") { api.eliminaLista(listaId) }

    suspend fun aggiungiItinerario(listaId: Long, itinerarioId: Long): Result<ListaPreferiti> =
        lista("Errore nell'aggiunta dell'itinerario") {
            api.aggiungiItinerario(listaId, PreferitoItinerarioRequestDto(itinerarioId))
        }

    suspend fun rimuoviItinerario(listaId: Long, itinerarioId: Long): Result<ListaPreferiti> =
        lista("Errore nella rimozione dell'itinerario") {
            api.rimuoviItinerario(listaId, itinerarioId)
        }

    suspend fun aggiungiAiPreferiti(itinerarioId: Long): Result<ListaPreferiti> =
        lista("Errore nel salvataggio fra i preferiti") {
            api.aggiungiAiPreferiti(PreferitoItinerarioRequestDto(itinerarioId))
        }


    suspend fun rimuoviDaiPreferiti(itinerarioId: Long): Result<Unit> =
        vuoto("Errore nella rimozione dai preferiti") { api.rimuoviDaiPreferiti(itinerarioId) }

    suspend fun condividiConEmail(listaId: Long, email: String): Result<ListaPreferiti> =
        lista("Errore nella condivisione della lista") {
            api.condividi(listaId, CondivisioneRequestDto(email = email))
        }

    suspend fun condividiConUtente(listaId: Long, utenteId: Long): Result<ListaPreferiti> =
        lista("Errore nella condivisione della lista") {
            api.condividi(listaId, CondivisioneRequestDto(utenteId = utenteId))
        }

    suspend fun revocaCondivisione(listaId: Long, utenteId: Long): Result<ListaPreferiti> =
        lista("Errore nella revoca della condivisione") {
            api.revocaCondivisione(listaId, utenteId)
        }

    private suspend fun elenco(
        errore: String,
        chiamata: suspend () -> Response<List<ListaPreferitiResponseDto>>
    ): Result<List<ListaPreferiti>> = try {
        val risposta = chiamata()
        // 404 su un elenco non e' un errore da mostrare: significa solo "niente da vedere".
        if (risposta.isSuccessful) {
            Result.success(risposta.body().orEmpty().map { it.toDomain() })
        } else if (risposta.code() == 404) {
            Result.success(emptyList())
        } else {
            Result.failure(Exception(messaggio(errore, risposta.code())))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun lista(
        errore: String,
        chiamata: suspend () -> Response<ListaPreferitiResponseDto>
    ): Result<ListaPreferiti> = try {
        val risposta = chiamata()
        val corpo = risposta.body()
        if (risposta.isSuccessful && corpo != null) {
            Result.success(corpo.toDomain())
        } else {
            Result.failure(Exception(messaggio(errore, risposta.code())))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun vuoto(
        errore: String,
        chiamata: suspend () -> Response<Unit>
    ): Result<Unit> = try {
        val risposta = chiamata()
        if (risposta.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(messaggio(errore, risposta.code())))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun messaggio(errore: String, codice: Int): String = when (codice) {
        400 -> "Dati non validi: controlla il nome della lista o l'indirizzo indicato"
        401 -> "Sessione scaduta: accedi di nuovo"
        403 -> "Questa lista non è tua: puoi solo consultarla"
        404 -> "Lista o utente non trovati"
        else -> "$errore (HTTP $codice)"
    }
}
