package com.example.travelapp.ui.catalog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.remote.ApiClient
import com.example.travelapp.data.remote.dto.ItinerarioRequestDto
import com.example.travelapp.data.repository.ItinerarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class CreaItinerarioUiState(
    val isSalvataggioInCorso: Boolean = false,
    val salvataggioCompletato: Boolean = false,
    val errorMessage: String? = null
)

class CreaItinerarioViewModel(
    private val repository: ItinerarioRepository = ItinerarioRepository(ApiClient.itinerarioApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreaItinerarioUiState())
    val uiState: StateFlow<CreaItinerarioUiState> = _uiState.asStateFlow()

    fun salvaItinerario(
        context: Context,
        idDaModificare: Long?,
        request: ItinerarioRequestDto,
        immagineUri: Uri?
    ) {
        _uiState.update { it.copy(isSalvataggioInCorso = true, errorMessage = null) }

        viewModelScope.launch {
            val result = if (idDaModificare != null) {
                repository.updateItinerario(idDaModificare, request)
            } else {
                repository.createItinerario(request)
            }

            if (result.isSuccess) {
                val itinerarioSalvato = result.getOrNull()

                // Upload immagine se selezionata
                if (immagineUri != null && itinerarioSalvato != null) {
                    caricaImmagineCopertina(context, itinerarioSalvato.id, immagineUri)
                }

                _uiState.update { it.copy(isSalvataggioInCorso = false, salvataggioCompletato = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSalvataggioInCorso = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Errore durante il salvataggio"
                    )
                }
            }
        }
    }

    private suspend fun caricaImmagineCopertina(context: Context, itinerarioId: Long, uri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bytes = inputStream.readBytes()
            inputStream.close()

            val type = contentResolver.getType(uri) ?: "image/jpeg"
            val requestBody = bytes.toRequestBody(type.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "copertina.jpg", requestBody)

            ApiClient.itinerarioApi.caricaImmagine(itinerarioId, part)
        } catch (_: Exception) {
            // Se fallisce l'upload immagine l'itinerario rimane comunque creato
        }
    }

    fun resetStato() {
        _uiState.value = CreaItinerarioUiState()
    }
}