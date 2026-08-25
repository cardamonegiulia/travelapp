package com.example.travelapp.ui.prenotazioni

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.repository.PagamentoRepository
import com.example.travelapp.data.repository.PrenotazioneRepository

class PrenotazioniViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(PrenotazioniViewModel::class.java)) {

            val prenotazioneRepository =
                PrenotazioneRepository(
                    ApiClient.getPrenotazioneApi(context)
                )

            val pagamentoRepository =
                PagamentoRepository(
                    ApiClient.getPagamentoApi(context)
                )

            return PrenotazioniViewModel(
                prenotazioneRepository = prenotazioneRepository,
                pagamentoRepository = pagamentoRepository
            ) as T
        }

        throw IllegalArgumentException(
            "ViewModel non supportato: ${modelClass.name}"
        )
    }
}