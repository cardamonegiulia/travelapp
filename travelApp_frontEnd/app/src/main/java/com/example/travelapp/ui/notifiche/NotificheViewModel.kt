package com.example.travelapp.ui.notifiche

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.NotificaRepository
import com.example.travelapp.domain.model.Notifica
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificheUiState(
    val notifiche: List<Notifica> = emptyList(),
    val nonLette: Long = 0,
    val isLoading: Boolean = false,
    val errore: String? = null
)

class NotificheViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: NotificaRepository =
        NotificaRepository(ApiClient.getNotificaApi(application))
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NotificheUiState())
    val uiState: StateFlow<NotificheUiState> = _uiState.asStateFlow()

    init {
        carica()
    }

    fun carica() {
        _uiState.update { it.copy(isLoading = true, errore = null) }

        viewModelScope.launch {
            val elenco = repository.getMieNotifiche()

            elenco
                .onSuccess { notifiche ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifiche = notifiche,
                            nonLette = notifiche.count { notifica -> !notifica.letta }.toLong()
                        )
                    }
                }
                .onFailure { errore ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errore = errore.message ?: "Errore nel caricamento delle notifiche"
                        )
                    }
                }
        }
    }

    fun aggiornaContatore() {
        viewModelScope.launch {
            repository.contaNonLette().onSuccess { quante ->
                _uiState.update { it.copy(nonLette = quante) }
            }
        }
    }

    fun segnaLetta(notifica: Notifica) {
        if (notifica.letta) return

        _uiState.update { stato ->
            stato.copy(
                notifiche = stato.notifiche.map {
                    if (it.id == notifica.id) it.copy(letta = true) else it
                },
                nonLette = (stato.nonLette - 1).coerceAtLeast(0)
            )
        }

        viewModelScope.launch {
            repository.segnaLetta(notifica.id)
        }
    }
}
