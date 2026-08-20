package com.example.travelapp.data.repository

import com.example.travelapp.data.remote.api.PagamentoApi
import com.example.travelapp.data.remote.dto.toDomain
import com.example.travelapp.domain.model.Pagamento

class PagamentoRepository(
    private val api: PagamentoApi
) {

    suspend fun pagaPrenotazione(prenotazioneId: Long): Pagamento {
        return api.pagaPrenotazione(prenotazioneId).toDomain()
    }

    suspend fun getMieiPagamenti(): List<Pagamento> {
        return api.getMieiPagamenti()
            .content
            .map { it.toDomain() }
    }
}