package com.example.travelapp.data.repository

import com.example.travelapp.data.remote.api.PrenotazioneApi
import com.example.travelapp.data.remote.dto.CreaPrenotazioneDto
import com.example.travelapp.domain.model.Prenotazione
import com.example.travelapp.data.remote.dto.toDomain

class PrenotazioneRepository(
    private val api: PrenotazioneApi
) {
    suspend fun getMiePrenotazioni(): List<Prenotazione> {
        return api.getMiePrenotazioni().content.map{it.toDomain()}
    }

    suspend fun getPrenotazione(id: Long): Prenotazione {
        return api.getPrenotazione(id).toDomain()
    }

    suspend fun creaPrenotazione(
        request: CreaPrenotazioneDto
    ): Prenotazione {
        return api.creaPrenotazione(request).toDomain()
    }

    suspend fun annullaPrenotazione(id: Long): Prenotazione {
        return api.annullaPrenotazione(id).toDomain()
    }
}