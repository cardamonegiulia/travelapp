package com.example.travelapp.ui.prenotazioni

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.PrenotazioneRepository
import com.example.travelapp.domain.model.Prenotazione
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingsListUiState(
    val prenotazioni: List<Prenotazione> = emptyList(),
    val prenotazioneSelezionata: Prenotazione? = null,
    val isLoading: Boolean = false,
    val errore: String? = null
)

class BookingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = PrenotazioneRepository(
        ApiClient.getPrenotazioneApi(application)
    )

    private val _uiState =
        MutableStateFlow(BookingsListUiState())

    val uiState: StateFlow<BookingsListUiState> =
        _uiState.asStateFlow()

    init {
        caricaPrenotazioni()
    }

    fun caricaPrenotazioni() {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errore = null
                )
            }

            try {
                val prenotazioni =
                    repository.getMiePrenotazioni()

                _uiState.update {
                    it.copy(
                        prenotazioni = prenotazioni,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errore = e.message
                            ?: "Errore nel caricamento delle prenotazioni"
                    )
                }
            }
        }
    }

    fun selezionaPrenotazione(
        prenotazione: Prenotazione
    ) {
        _uiState.update {
            it.copy(
                prenotazioneSelezionata = prenotazione
            )
        }
    }

    fun chiudiDettaglio() {
        _uiState.update {
            it.copy(
                prenotazioneSelezionata = null
            )
        }
    }

    fun annullaPrenotazione() {

        val prenotazione =
            _uiState.value.prenotazioneSelezionata
                ?: return

        if (_uiState.value.isLoading) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errore = null
                )
            }

            try {

                val aggiornata =
                    repository.annullaPrenotazione(
                        prenotazione.id
                    )

                _uiState.update { stato ->

                    stato.copy(
                        prenotazioni =
                            stato.prenotazioni.map {
                                if (it.id == aggiornata.id) {
                                    aggiornata
                                } else {
                                    it
                                }
                            },
                        prenotazioneSelezionata = aggiornata,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errore = e.message
                            ?: "Errore durante l'annullamento"
                    )
                }
            }
        }
    }
}