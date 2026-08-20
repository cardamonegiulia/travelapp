package  com.example.travelapp.data.remote.dto

import com.example.travelapp.domain.model.Pagamento
import com.example.travelapp.domain.model.StatoPagamento
import com.example.travelapp.domain.model.StatoPrenotazione

data class PagamentoDto(
    val idPagamento: Long,
    val prenotazioneId: Long,
    val importo: Double,
    val statoPagamento: StatoPagamento,
    val statoPrenotazione: StatoPrenotazione,
    val dataPagamento: String?
)

fun PagamentoDto.toDomain(): Pagamento {
    return Pagamento(
        id = idPagamento,
        prenotazioneId = prenotazioneId,
        importo = importo,
        statoPagamento = statoPagamento,
        statoPrenotazione = statoPrenotazione,
        dataPagamento = dataPagamento
    )
}