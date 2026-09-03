package com.example.travelapp.ui.catalog
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
enum class TipoFiltroCatalogo {
    TUTTI,
    ITINERARI,
    ATTIVITA
}
data class CatalogUiState(
    val isLoading: Boolean = false,
    val itinerari: List<Itinerario> = emptyList(),
    val attivita: List<SingolaAttivita> = emptyList(),
    val filtroSelezionato: TipoFiltroCatalogo = TipoFiltroCatalogo.TUTTI,
    val queryRicerca: String = "",
    val errorMessage: String? = null
)
