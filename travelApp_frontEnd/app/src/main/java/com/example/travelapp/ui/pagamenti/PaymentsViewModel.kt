package com.example.travelapp.ui.pagamenti

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.PagamentoRepository
import com.example.travelapp.domain.model.Pagamento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentsUiState(
    val pagamenti: List<Pagamento> = emptyList(),
    val isLoading: Boolean = false,
    val errore: String? = null
)

class PaymentsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = PagamentoRepository(
        ApiClient.getPagamentoApi(application)
    )

    private val _uiState =
        MutableStateFlow(PaymentsUiState())

    val uiState: StateFlow<PaymentsUiState> =
        _uiState.asStateFlow()

    init {
        caricaPagamenti()
    }

    fun caricaPagamenti() {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errore = null
                )
            }

            try {

                val pagamenti =
                    repository.getMieiPagamenti()

                _uiState.update {
                    it.copy(
                        pagamenti = pagamenti,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errore = e.message
                            ?: "Errore nel caricamento dei pagamenti"
                    )
                }
            }
        }
    }
}